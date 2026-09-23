#!/usr/bin/env bash
set -euo pipefail

# ──────────────────────────────────────────────
#  CredFolha — Demo passo a passo
#  Requer: serviços rodando (make up-all)
#  Gravar: make demo-record  (asciinema)
# ──────────────────────────────────────────────

BOLD='\033[1m'
CYAN='\033[36m'
GREEN='\033[32m'
YELLOW='\033[33m'
RED='\033[31m'
RESET='\033[0m'

step=0

banner() {
  step=$((step + 1))
  echo ""
  echo -e "${BOLD}${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
  echo -e "${BOLD}${CYAN}  PASSO ${step}: $1${RESET}"
  echo -e "${BOLD}${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
  echo ""
}

explain() {
  echo -e "  ${YELLOW}▸ $1${RESET}"
}

run_cmd() {
  echo -e "  ${BOLD}\$ $1${RESET}"
  eval "$1"
  echo ""
}

pause() {
  local secs=${1:-3}
  echo -e "  ${GREEN}⏳ Aguardando ${secs}s para o processamento assíncrono...${RESET}"
  sleep "$secs"
}

# ──────────────────────────────────────────────

echo -e "${BOLD}"
echo "  ╔═══════════════════════════════════════════════════╗"
echo "  ║        CredFolha — Demonstração Completa          ║"
echo "  ║  Empréstimo Consignado com Sagas Coreografadas    ║"
echo "  ╚═══════════════════════════════════════════════════╝"
echo -e "${RESET}"
sleep 2

# ── 1. Estado inicial ─────────────────────────
banner "Verificar serviços e banco"
explain "Todos os containers devem estar healthy."
run_cmd "docker compose --profile domain ps --format 'table {{.Name}}\t{{.Status}}'"
pause 1

explain "Banco limpo — tabelas vazias."
run_cmd "docker exec postgres psql -U postgres -d aed -c 'SELECT count(*) AS emprestimos FROM emprestimo;'"
run_cmd "docker exec postgres psql -U postgres -d aed -c 'SELECT count(*) AS lancamentos_margem FROM margem;'"

# ── 2. Fluxo feliz ────────────────────────────
banner "Fluxo feliz — aprovação de crédito"
explain "CPF 44444444444 (dígito par → análise aprova)."
explain "POST /emprestimos/solicitar → 202 Accepted."
explain "Kafka: emprestimo.solicitado.v1 → margem.reservada.v1 → analise.aprovada.v1"
run_cmd "cd request && ./make_request.sh cpf_analise_aprovada.json"
pause 5

explain "Resultado no banco: empréstimo disponibilizado, margem debitada, análise aprovada."
run_cmd "docker exec postgres psql -U postgres -d aed -c 'SELECT id, cpf, status FROM emprestimo ORDER BY data_emprestimo DESC LIMIT 3;'"
run_cmd "docker exec postgres psql -U postgres -d aed -c 'SELECT cpf, valor, tipo FROM margem ORDER BY criado_em DESC LIMIT 3;'"
run_cmd "docker exec postgres psql -U postgres -d aed -c 'SELECT cpf, resultado FROM analise ORDER BY criado_em DESC LIMIT 3;'"

# ── 3. Saga — compensação ─────────────────────
banner "Saga — reprovação + compensação de margem"
explain "CPF 33333333333 (dígito ímpar → análise reprova)."
explain "Margem é reservada (DÉBITO), depois análise reprova."
explain "Compensação: MargemCompensacaoListener insere CRÉDITO e publica margem.liberada.v1."
run_cmd "cd request && ./make_request.sh cpf_analise_reprovada.json"
pause 5

explain "Resultado: dois lançamentos para o mesmo CPF — DÉBITO + CRÉDITO (saldo volta a zero)."
run_cmd "docker exec postgres psql -U postgres -d aed -c \"SELECT cpf, valor, tipo, criado_em FROM margem WHERE cpf = '33333333333' ORDER BY criado_em;\""
run_cmd "docker exec postgres psql -U postgres -d aed -c \"SELECT cpf, SUM(valor) AS saldo FROM margem WHERE cpf = '33333333333' GROUP BY cpf;\""

# ── 4. Margem insuficiente ────────────────────
banner "Margem insuficiente — recusa sem análise"
explain "CPF 92312348312, parcela de R\$ 3.000 excede a margem."
explain "sistema-margem recusa direto, sem acionar análise de crédito."
run_cmd "cd request && ./make_request.sh cpf_margem_insuficiente.json"
pause 3

explain "Resultado: nenhum lançamento de margem para este CPF."
run_cmd "docker exec postgres psql -U postgres -d aed -c \"SELECT cpf, status FROM emprestimo WHERE cpf = '92312348312' ORDER BY data_emprestimo DESC LIMIT 1;\""

# ── 5. DLQ — falha transitória ────────────────
banner "DLQ — falha transitória (banco indisponível)"
explain "CPF 55555555555 aciona simulação de erro no MargemListener."
explain "9 tentativas com backoff (1s, 2s, 4s, 8s, 16s, 30s, 30s, 30s) → ~2 min → DLQ."
explain "Acompanhe os retries: make logs-margem (em outro terminal)."
run_cmd "cd request && ./make_request.sh cpf_banco_indisponivel.json"
explain "Evento será encaminhado para emprestimo.solicitado.v1.dlq em ~2 minutos."
explain "Verifique no Kafka UI: http://localhost:8089"
pause 5

# ── 6. Idempotência ───────────────────────────
banner "Idempotência — reprocessamento seguro"
explain "Disparando o MESMO cenário de aprovação novamente."
explain "Se o ce_id já foi processado, o consumidor descarta em silêncio."
explain "(Nota: a API gera novo ce_id a cada POST, então este teste mostra"
explain " que duas solicitações distintas para o mesmo CPF são processadas.)"
run_cmd "cd request && ./make_request.sh cpf_analise_aprovada.json"
pause 5
run_cmd "docker exec postgres psql -U postgres -d aed -c \"SELECT cpf, SUM(valor) AS saldo_margem, COUNT(*) AS lancamentos FROM margem GROUP BY cpf ORDER BY cpf;\""

# ── Fim ───────────────────────────────────────
echo ""
echo -e "${BOLD}${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
echo -e "${BOLD}${GREEN}  Demo concluída.${RESET}"
echo -e "${BOLD}${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
echo ""
echo -e "  Comandos úteis:"
echo -e "    ${CYAN}make db-saldo${RESET}          — saldo de margem por CPF"
echo -e "    ${CYAN}make logs${RESET}              — logs dos 3 serviços"
echo -e "    ${CYAN}make down${RESET}              — derruba tudo"
echo -e "    ${CYAN}make down-clean${RESET}        — derruba tudo + apaga volumes"
echo ""
