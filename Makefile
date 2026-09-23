.DEFAULT_GOAL := help
SHELL := /bin/bash

COMPOSE := docker compose
PROFILE := --profile domain

# ──────────────────────────────────────────────
#  Infraestrutura
# ──────────────────────────────────────────────

.PHONY: up
up: ## Sobe infraestrutura (Kafka, Postgres, Kafka UI)
	$(COMPOSE) up -d

.PHONY: up-all
up-all: ## Sobe infraestrutura + os 3 serviços Java em containers
	$(COMPOSE) $(PROFILE) up -d --build

.PHONY: down
down: ## Derruba tudo (containers e rede)
	$(COMPOSE) $(PROFILE) down

.PHONY: down-clean
down-clean: ## Derruba tudo e apaga volumes (banco e kafka)
	$(COMPOSE) $(PROFILE) down -v

.PHONY: ps
ps: ## Mostra estado dos containers
	$(COMPOSE) $(PROFILE) ps

.PHONY: rebuild
rebuild: ## Rebuild e restart dos 3 serviços Java (infra mantida)
	$(COMPOSE) $(PROFILE) up -d --build sistema-emprestimo sistema-margem sistema-analise

# ──────────────────────────────────────────────
#  Logs
# ──────────────────────────────────────────────

.PHONY: logs-emprestimo
logs-emprestimo: ## Logs do sistema-emprestimo (Ctrl+C sai)
	$(COMPOSE) logs -f --tail 50 sistema-emprestimo

.PHONY: logs-margem
logs-margem: ## Logs do sistema-margem (Ctrl+C sai)
	$(COMPOSE) logs -f --tail 50 sistema-margem

.PHONY: logs-analise
logs-analise: ## Logs do sistema-analise (Ctrl+C sai)
	$(COMPOSE) logs -f --tail 50 sistema-analise

.PHONY: logs
logs: ## Logs de todos os serviços de domínio (Ctrl+C sai)
	$(COMPOSE) logs -f --tail 50 sistema-emprestimo sistema-margem sistema-analise

# ──────────────────────────────────────────────
#  Cenários de teste
# ──────────────────────────────────────────────

.PHONY: test-aprovado
test-aprovado: ## Fluxo feliz: margem ok, análise de crédito aprova
	@echo "━━━ Cenário: análise APROVADA (CPF 44444444444, dígito par) ━━━"
	@cd request && ./make_request.sh cpf_analise_aprovada.json

.PHONY: test-reprovado
test-reprovado: ## Margem ok, análise reprova → compensação (Saga)
	@echo "━━━ Cenário: análise REPROVADA + compensação (CPF 33333333333, dígito ímpar) ━━━"
	@cd request && ./make_request.sh cpf_analise_reprovada.json

.PHONY: test-margem
test-margem: ## Margem insuficiente → recusada sem análise
	@echo "━━━ Cenário: margem INSUFICIENTE (CPF 92312348312) ━━━"
	@cd request && ./make_request.sh cpf_margem_insuficiente.json

.PHONY: test-dlq
test-dlq: ## Falha transitória → retentativas → DLQ (~2 min)
	@echo "━━━ Cenário: banco INDISPONÍVEL → 9 tentativas → DLQ (CPF 55555555555) ━━━"
	@echo "Acompanhe com: make logs-margem"
	@cd request && ./make_request.sh cpf_banco_indisponivel.json

.PHONY: test-malformado
test-malformado: ## JSON malformado direto no tópico → DLQ sem retentativa
	@echo "━━━ Cenário: JSON MALFORMADO → DLQ imediata (sem retentativa) ━━━"
	docker exec -i kafka kafka-console-producer --bootstrap-server localhost:29092 \
	  --topic emprestimo.solicitado.v1 --property parse.headers=true --property parse.key=true \
	  < request/emprestimo_solicitado_malformado.txt
	@echo "Verifique no Kafka UI: http://localhost:8089 → emprestimo.solicitado.v1.dlq"

# ──────────────────────────────────────────────
#  Consultas no banco
# ──────────────────────────────────────────────

.PHONY: db-emprestimo
db-emprestimo: ## Últimos 10 empréstimos
	docker exec postgres psql -U postgres -d aed -c \
	  "SELECT * FROM emprestimo ORDER BY data_emprestimo DESC LIMIT 10;"

.PHONY: db-margem
db-margem: ## Últimos 10 lançamentos de margem (débitos e créditos)
	docker exec postgres psql -U postgres -d aed -c \
	  "SELECT * FROM margem ORDER BY criado_em DESC LIMIT 10;"

.PHONY: db-analise
db-analise: ## Últimas 10 análises de crédito
	docker exec postgres psql -U postgres -d aed -c \
	  "SELECT * FROM analise ORDER BY criado_em DESC LIMIT 10;"

.PHONY: db-saldo
db-saldo: ## Saldo de margem por CPF (débitos - créditos)
	docker exec postgres psql -U postgres -d aed -c \
	  "SELECT cpf, SUM(valor) AS saldo_margem, COUNT(*) AS lancamentos FROM margem GROUP BY cpf ORDER BY cpf;"

# ──────────────────────────────────────────────
#  Testes unitários
# ──────────────────────────────────────────────

.PHONY: test
test: ## Roda testes dos 3 serviços (sem Docker, precisa de Java 21)
	cd sistema-emprestimo && ./mvnw -q test
	cd sistema-margem && ./mvnw -q test
	cd sistema-analise && ./mvnw -q test

# ──────────────────────────────────────────────
#  Demo com asciinema
# ──────────────────────────────────────────────

.PHONY: demo
demo: ## Roda a demo completa com pausas (requer serviços rodando)
	@./scripts/demo.sh

.PHONY: demo-record
demo-record: ## Grava a demo com asciinema (gera demo.cast)
	asciinema rec --title "CredFolha — Demo completa" \
	  --idle-time-limit 3 \
	  -c "make demo" \
	  demo.cast

.PHONY: demo-play
demo-play: ## Reproduz a gravação da demo
	asciinema play demo.cast

# ──────────────────────────────────────────────
#  Help
# ──────────────────────────────────────────────

.PHONY: help
help: ## Mostra esta ajuda
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | \
	  awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-18s\033[0m %s\n", $$1, $$2}'
