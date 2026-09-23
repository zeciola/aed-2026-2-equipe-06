#!/usr/bin/env bash
set -euo pipefail

# ──────────────────────────────────────────────
#  CredFolha — Demo passo a passo
#  Requer: serviços rodando (make up-all)
# ──────────────────────────────────────────────

BOLD='\033[1m'
CYAN='\033[36m'
GREEN='\033[32m'
YELLOW='\033[33m'
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
  local display="${2:-$1}"
  echo -e "  ${BOLD}\$ ${display}${RESET}"
  eval "$1"
  echo ""
}

pause() {
  local secs=${1:-3}
  echo -e "  ${GREEN}⏳ Aguardando ${secs}s para o processamento assíncrono...${RESET}"
  sleep "$secs"
  echo ""
}

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DB="docker exec postgres psql -U postgres -d aed -c"
REQUEST="$REPO_ROOT/request"

# ──────────────────────────────────────────────

echo -e "${BOLD}"
echo "  ╔═══════════════════════════════════════════════════╗"
echo "  ║        CredFolha — Demonstração Completa          ║"
echo "  ║  Empréstimo Consignado com Sagas Coreografadas    ║"
echo "  ╚═══════════════════════════════════════════════════╝"
echo -e "${RESET}"
sleep 2

# ── 1. Estado inicial ─────────────────────────
banner "Verificar serviços e saldo inicial de margem"
explain "Containers rodando:"
run_cmd "docker compose --profile domain ps --format 'table {{.Name}}\t{{.Status}}'"
pause 1

explain "Saldo de margem por CPF (data.sql insere crédito inicial para cada cliente):"
run_cmd "$DB \"SELECT cpf, SUM(valor) AS saldo, COUNT(*) AS lancamentos FROM margem GROUP BY cpf ORDER BY cpf;\""

explain "Nenhum empréstimo solicitado ainda:"
run_cmd "$DB \"SELECT count(*) AS emprestimos FROM emprestimo;\""

# ── 2. Fluxo feliz ────────────────────────────
banner "Fluxo feliz — aprovação de crédito"
explain "CPF 44444444444 (dígito par → análise aprova)."
explain "POST /emprestimos/solicitar → 202 Accepted."
explain "Kafka: emprestimo.solicitado.v1 → margem.reservada.v1 → analise.aprovada.v1"
run_cmd "$REQUEST/make_request.sh $REQUEST/cpf_analise_aprovada.json" "request/make_request.sh cpf_analise_aprovada.json"
pause 8

explain "Resultado: empréstimo registrado."
run_cmd "$DB \"SELECT id, cpf, valor_parcela, valor_total FROM emprestimo ORDER BY data_emprestimo DESC LIMIT 3;\""

explain "Margem debitada (DEBITO de R\$ 100,00)."
run_cmd "$DB \"SELECT cpf, valor, tipo FROM margem WHERE cpf = '44444444444' ORDER BY criado_em;\""

explain "Análise aprovada."
run_cmd "$DB \"SELECT cpf, aprovada FROM analise ORDER BY criado_em DESC LIMIT 3;\""

# ── 3. Saga — compensação ─────────────────────
banner "Saga — reprovação + compensação de margem"
explain "CPF 33333333333 (dígito ímpar → análise reprova)."
explain "Margem reservada (DÉBITO), análise reprova, compensação insere CRÉDITO."
explain "Evento: analise.reprovada.v1 → MargemCompensacaoListener → margem.liberada.v1"
run_cmd "$REQUEST/make_request.sh $REQUEST/cpf_analise_reprovada.json" "request/make_request.sh cpf_analise_reprovada.json"
pause 8

explain "Dois lançamentos: DÉBITO (reserva) + CRÉDITO (compensação)."
run_cmd "$DB \"SELECT cpf, valor, tipo, criado_em FROM margem WHERE cpf = '33333333333' ORDER BY criado_em;\""

explain "Saldo volta ao valor original (compensação anulou o débito)."
run_cmd "$DB \"SELECT cpf, SUM(valor) AS saldo FROM margem WHERE cpf = '33333333333' GROUP BY cpf;\""

# ── 4. Margem insuficiente ────────────────────
banner "Margem insuficiente — recusa sem análise"
explain "CPF 92312348312, parcela de R\$ 3.000 excede a margem disponível."
explain "sistema-margem recusa direto, sem acionar análise de crédito."
run_cmd "$REQUEST/make_request.sh $REQUEST/cpf_margem_insuficiente.json" "request/make_request.sh cpf_margem_insuficiente.json"
pause 5

explain "Empréstimo registrado, mas nenhuma reserva de margem criada para este CPF."
run_cmd "$DB \"SELECT cpf, valor, tipo FROM margem WHERE cpf = '92312348312';\""

# ── 5. Resumo final ──────────────────────────
banner "Resumo — saldo de margem por CPF"
run_cmd "$DB \"SELECT cpf, SUM(valor) AS saldo, COUNT(*) AS lancamentos FROM margem GROUP BY cpf ORDER BY cpf;\""

# ── Fim ───────────────────────────────────────
echo ""
echo -e "${BOLD}${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
echo -e "${BOLD}${GREEN}  Demo concluída.${RESET}"
echo -e "${BOLD}${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
echo ""
echo -e "  Cenário DLQ (make test-dlq) omitido da demo por durar ~2 min."
echo -e "  Kafka UI: ${CYAN}http://localhost:8089${RESET}"
echo ""
