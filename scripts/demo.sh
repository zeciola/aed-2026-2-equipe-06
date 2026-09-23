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
WHITE='\033[97m'
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

info() {
  echo -e "  ${WHITE}$1${RESET}"
}

run_cmd() {
  local display="${2:-$1}"
  echo -e "  ${BOLD}\$ ${display}${RESET}"
  eval "$1"
  echo ""
}

pause() {
  local secs=${1:-3}
  echo -e "  ${GREEN}⏳ ${secs}s...${RESET}"
  sleep "$secs"
  echo ""
}

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DB="docker exec postgres psql -U postgres -d aed -c"
REQUEST="$REPO_ROOT/request"
KAFKA="docker exec kafka"

kafka_offsets() {
  for t in "$@"; do
    $KAFKA kafka-get-offsets --bootstrap-server localhost:29092 --topic "$t" 2>/dev/null \
      | awk -F: '{sum+=$3} END {printf "  %-35s %d evento(s)\n", "'"$t"'", sum}'
  done
}

# ──────────────────────────────────────────────

echo -e "${BOLD}"
echo "  ╔═══════════════════════════════════════════════════╗"
echo "  ║        CredFolha — Demonstração Completa          ║"
echo "  ║  Empréstimo Consignado com Sagas Coreografadas    ║"
echo "  ╚═══════════════════════════════════════════════════╝"
echo -e "${RESET}"
info "Plataforma de crédito consignado com 3 serviços independentes"
info "comunicando-se por eventos de domínio (CloudEvents 1.0) no Apache Kafka."
info ""
info "Serviços: sistema-emprestimo, sistema-margem, sistema-analise"
info "Infra: Kafka (broker), Postgres (banco único, schemas separados), Kafka UI"
sleep 5

# ── 1. Estado inicial ─────────────────────────
banner "Verificar serviços e estado inicial"

explain "Todos os 6 containers devem estar rodando."
explain "Os 3 serviços Java sobem via Docker multi-stage build (sem Java local)."
run_cmd "docker compose --profile domain ps --format 'table {{.Name}}\t{{.Status}}'"
pause 4

explain "O data.sql do sistema-margem insere crédito inicial para cada CPF."
explain "Esse saldo representa a margem consignável disponível do servidor público."
run_cmd "$DB \"SELECT cpf, SUM(valor) AS saldo FROM margem GROUP BY cpf ORDER BY cpf;\""
pause 4

explain "Nenhum empréstimo foi solicitado ainda — tabela vazia."
run_cmd "$DB \"SELECT count(*) AS emprestimos FROM emprestimo;\""
pause 3

explain "Tópicos Kafka criados automaticamente pelos serviços no startup:"
run_cmd "$KAFKA kafka-topics --bootstrap-server localhost:29092 --list 2>/dev/null | grep -E '^(emprestimo|margem|analise)'" "kafka-topics --list | grep emprestimo|margem|analise"
info "Cada tópico tem 3 partições. A chave de partição é o CPF do cliente,"
info "garantindo ordenação de eventos por cliente."
pause 5

# ── 2. Fluxo feliz ────────────────────────────
banner "Fluxo feliz — aprovação de crédito"

explain "Cenário: solicitação de empréstimo que será APROVADA."
explain "CPF 44444444444 — último dígito par → regra determinística aprova."
echo ""
info "Cadeia de eventos (saga coreografada, sem orquestrador):"
info ""
info "  Cliente → POST /emprestimos/solicitar (HTTP 202)"
info "       │"
info "       ▼"
info "  sistema-emprestimo grava no banco e publica:"
info "       emprestimo.solicitado.v1"
info "       │"
info "       ▼"
info "  sistema-margem consome, reserva margem (DÉBITO) e publica:"
info "       margem.reservada.v1"
info "       │"
info "       ▼"
info "  sistema-analise consome, aprova e publica:"
info "       analise.aprovada.v1"
info "       │"
info "       ▼"
info "  sistema-emprestimo consome e disponibiliza o empréstimo."
pause 8

explain "Enviando a solicitação..."
run_cmd "$REQUEST/make_request.sh $REQUEST/cpf_analise_aprovada.json" "request/make_request.sh cpf_analise_aprovada.json"
info "HTTP 202 Accepted — processamento assíncrono via Kafka."
pause 10

explain "Empréstimo registrado no banco pelo sistema-emprestimo:"
run_cmd "$DB \"SELECT id, cpf, valor_parcela, valor_total FROM emprestimo ORDER BY data_emprestimo DESC LIMIT 1;\""
pause 4

explain "Margem reservada pelo sistema-margem — lançamento DÉBITO de R\$ 100:"
run_cmd "$DB \"SELECT cpf, valor, tipo FROM margem WHERE cpf = '44444444444' ORDER BY criado_em;\""
info "Linha 1: crédito inicial (data.sql). Linha 2: débito da reserva."
pause 5

explain "Análise de crédito aprovada pelo sistema-analise:"
run_cmd "$DB \"SELECT cpf, aprovada FROM analise WHERE cpf = '44444444444';\""
pause 4

explain "Eventos no Kafka (soma de todas as partições por tópico):"
kafka_offsets emprestimo.solicitado.v1 margem.reservada.v1 analise.aprovada.v1
echo ""
info "1 evento em cada tópico: a cadeia completa funcionou."
pause 5

# ── 3. Saga — compensação ─────────────────────
banner "Saga — reprovação + compensação automática"

explain "Cenário: solicitação que será REPROVADA na análise de crédito."
explain "CPF 33333333333 — último dígito ímpar → regra determinística reprova."
echo ""
info "O que acontece quando a análise reprova:"
info ""
info "  1. sistema-emprestimo publica emprestimo.solicitado.v1"
info "  2. sistema-margem reserva margem (DÉBITO) e publica margem.reservada.v1"
info "  3. sistema-analise reprova e publica analise.reprovada.v1"
info "  4. MargemCompensacaoListener (sistema-margem) consome a reprovação"
info "     e insere lançamento CRÉDITO para anular o débito (ledger imutável)"
info "  5. sistema-margem publica margem.liberada.v1 como fato de compensação"
info ""
info "A compensação usa INSERT (crédito), nunca UPDATE/DELETE no débito original"
info "— preserva a trilha de auditoria completa."
pause 10

explain "Enviando a solicitação..."
run_cmd "$REQUEST/make_request.sh $REQUEST/cpf_analise_reprovada.json" "request/make_request.sh cpf_analise_reprovada.json"
info "HTTP 202 — saga completa (reserva + reprovação + compensação) é assíncrona."
pause 12

explain "Lançamentos de margem para o CPF 33333333333:"
run_cmd "$DB \"SELECT cpf, valor, tipo, criado_em FROM margem WHERE cpf = '33333333333' ORDER BY criado_em;\""
info "Linha 1: crédito inicial (data.sql)."
info "Linha 2: DÉBITO — reserva de margem pelo MargemListener."
info "Linha 3: CRÉDITO — compensação pelo MargemCompensacaoListener."
info "O débito e o crédito se anulam por soma algébrica."
pause 7

explain "Prova: saldo volta ao valor original (R\$ 500)."
run_cmd "$DB \"SELECT cpf, SUM(valor) AS saldo FROM margem WHERE cpf = '33333333333' GROUP BY cpf;\""
pause 4

explain "Kafka confirma: eventos de reprovação e compensação publicados."
kafka_offsets analise.reprovada.v1 margem.liberada.v1
echo ""
info "1 evento em analise.reprovada.v1 → disparou a compensação."
info "1 evento em margem.liberada.v1 → fato de compensação publicado."
pause 6

# ── 4. Margem insuficiente ────────────────────
banner "Margem insuficiente — recusa imediata"

explain "Cenário: parcela de R\$ 3.000 excede a margem disponível."
explain "CPF 92312348312 — não tem crédito no data.sql (margem = R\$ 0)."
echo ""
info "Neste caso, o sistema-margem recusa a reserva direto"
info "e publica margem.recusada.v1 — a análise de crédito NÃO é acionada."
info "É uma decisão de negócio: sem margem, não faz sentido analisar crédito."
pause 6

explain "Enviando a solicitação..."
run_cmd "$REQUEST/make_request.sh $REQUEST/cpf_margem_insuficiente.json" "request/make_request.sh cpf_margem_insuficiente.json"
pause 8

explain "Nenhuma reserva de margem criada — CPF sem lançamentos:"
run_cmd "$DB \"SELECT cpf, valor, tipo FROM margem WHERE cpf = '92312348312';\""
info "0 rows — a reserva foi recusada, nenhum débito registrado."
pause 5

# ── 5. Resumo final ──────────────────────────
banner "Resumo — saldo final e visão do Kafka"

explain "Saldo de margem por CPF após os 3 cenários:"
run_cmd "$DB \"SELECT cpf, SUM(valor) AS saldo, COUNT(*) AS lancamentos FROM margem GROUP BY cpf ORDER BY cpf;\""
info "CPF 33333333333: saldo R\$ 500 (voltou ao original após compensação)."
info "CPF 44444444444: saldo R\$ 1.900 (R\$ 2.000 - R\$ 100 da reserva aprovada)."
info "Demais CPFs: inalterados."
pause 7

explain "Visão completa dos tópicos Kafka — total de eventos por tópico:"
kafka_offsets emprestimo.solicitado.v1 margem.reservada.v1 margem.liberada.v1 analise.aprovada.v1 analise.reprovada.v1
echo ""
info "3 solicitações → 2 reservas de margem → 1 aprovação + 1 reprovação + 1 compensação."
info "O fluxo de eventos é consistente com os cenários demonstrados."
pause 7

# ── Fim ───────────────────────────────────────
echo ""
echo -e "${BOLD}${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
echo -e "${BOLD}${GREEN}  Demo concluída com sucesso.${RESET}"
echo -e "${BOLD}${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
echo ""
info "Padrões demonstrados:"
info "  • Saga coreografada (sem orquestrador central)"
info "  • Compensação por ledger imutável (INSERT, nunca UPDATE/DELETE)"
info "  • Idempotência via INSERT ON CONFLICT DO NOTHING (ce_id)"
info "  • CloudEvents 1.0 em modo binary (headers Kafka)"
info "  • DLQ com backoff exponencial (make test-dlq, ~2 min)"
echo ""
info "Cenário DLQ omitido desta demo por durar ~2 min."
info "Kafka UI disponível em: http://localhost:8089"
echo ""
