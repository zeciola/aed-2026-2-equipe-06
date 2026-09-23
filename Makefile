.DEFAULT_GOAL := help
SHELL := /bin/bash

COMPOSE := docker compose
PROFILE := --profile domain
WAIT    := 5

# ──────────────────────────────────────────────
#  Tudo de uma vez
# ──────────────────────────────────────────────

.PHONY: all
all: up-all wait-ready validate ## Sobe tudo, espera e valida os 4 cenários

.PHONY: wait-ready
wait-ready:
	@echo "⏳ Aguardando serviços ficarem prontos..."
	@for i in $$(seq 1 60); do \
	  code=$$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/emprestimos/solicitar 2>/dev/null); \
	  if [ "$$code" = "405" ] || [ "$$code" = "400" ] || [ "$$code" = "200" ] || [ "$$code" = "202" ]; then \
	    echo "✔ sistema-emprestimo respondendo (HTTP $$code)"; break; \
	  fi; \
	  sleep 2; \
	done

.PHONY: validate
validate: ## Dispara os 4 cenários e mostra resultado no banco
	@echo ""
	@echo "━━━ 1/4  Fluxo feliz (aprovação) ━━━"
	@cd request && ./make_request.sh cpf_analise_aprovada.json
	@sleep $(WAIT)
	@docker exec postgres psql -U postgres -d aed -c \
	  "SELECT cpf, valor, tipo FROM margem ORDER BY criado_em DESC LIMIT 3;"
	@echo ""
	@echo "━━━ 2/4  Saga — reprovação + compensação ━━━"
	@cd request && ./make_request.sh cpf_analise_reprovada.json
	@sleep $(WAIT)
	@docker exec postgres psql -U postgres -d aed -c \
	  "SELECT cpf, valor, tipo FROM margem WHERE cpf = '33333333333' ORDER BY criado_em;"
	@echo ""
	@echo "━━━ 3/4  Margem insuficiente ━━━"
	@cd request && ./make_request.sh cpf_margem_insuficiente.json
	@sleep 3
	@echo ""
	@echo "━━━ 4/4  JSON malformado → DLQ ━━━"
	@docker exec -i kafka kafka-console-producer --bootstrap-server localhost:29092 \
	  --topic emprestimo.solicitado.v1 --property parse.headers=true --property parse.key=true \
	  < request/emprestimo_solicitado_malformado.txt
	@sleep 2
	@echo ""
	@echo "━━━ Resumo: saldo de margem por CPF ━━━"
	@docker exec postgres psql -U postgres -d aed -c \
	  "SELECT cpf, SUM(valor) AS saldo, COUNT(*) AS lancamentos FROM margem GROUP BY cpf ORDER BY cpf;"
	@echo ""
	@echo "✔ Validação concluída. Kafka UI: http://localhost:8089"

# ──────────────────────────────────────────────
#  Infraestrutura
# ──────────────────────────────────────────────

.PHONY: up
up: ## Sobe só infraestrutura (Kafka, Postgres, Kafka UI)
	$(COMPOSE) up -d

.PHONY: up-all
up-all: ## Sobe infraestrutura + os 3 serviços Java
	$(COMPOSE) $(PROFILE) up -d --build

.PHONY: down
down: ## Derruba tudo
	$(COMPOSE) $(PROFILE) down

.PHONY: down-clean
down-clean: ## Derruba tudo e apaga volumes
	$(COMPOSE) $(PROFILE) down -v

.PHONY: ps
ps: ## Estado dos containers
	$(COMPOSE) $(PROFILE) ps

.PHONY: rebuild
rebuild: ## Rebuild dos serviços Java (infra mantida)
	$(COMPOSE) $(PROFILE) up -d --build sistema-emprestimo sistema-margem sistema-analise

# ──────────────────────────────────────────────
#  Logs e banco
# ──────────────────────────────────────────────

.PHONY: logs
logs: ## Logs dos 3 serviços (Ctrl+C sai)
	$(COMPOSE) logs -f --tail 50 sistema-emprestimo sistema-margem sistema-analise

.PHONY: db
db: ## Saldo de margem por CPF + últimos lançamentos
	@docker exec postgres psql -U postgres -d aed -c \
	  "SELECT cpf, SUM(valor) AS saldo, COUNT(*) AS lancamentos FROM margem GROUP BY cpf ORDER BY cpf;"
	@docker exec postgres psql -U postgres -d aed -c \
	  "SELECT cpf, valor, tipo, criado_em FROM margem ORDER BY criado_em DESC LIMIT 10;"

# ──────────────────────────────────────────────
#  Cenários individuais
# ──────────────────────────────────────────────

.PHONY: test-aprovado
test-aprovado: ## Fluxo feliz: análise aprova
	@cd request && ./make_request.sh cpf_analise_aprovada.json

.PHONY: test-reprovado
test-reprovado: ## Análise reprova → compensação (Saga)
	@cd request && ./make_request.sh cpf_analise_reprovada.json

.PHONY: test-margem
test-margem: ## Margem insuficiente
	@cd request && ./make_request.sh cpf_margem_insuficiente.json

.PHONY: test-dlq
test-dlq: ## Falha transitória → DLQ (~2 min)
	@cd request && ./make_request.sh cpf_banco_indisponivel.json

.PHONY: test-malformado
test-malformado: ## JSON malformado → DLQ imediata
	@docker exec -i kafka kafka-console-producer --bootstrap-server localhost:29092 \
	  --topic emprestimo.solicitado.v1 --property parse.headers=true --property parse.key=true \
	  < request/emprestimo_solicitado_malformado.txt

# ──────────────────────────────────────────────
#  Testes unitários e demo
# ──────────────────────────────────────────────

.PHONY: test
test: ## Testes unitários (precisa de Java 21)
	cd sistema-emprestimo && ./mvnw -q test
	cd sistema-margem && ./mvnw -q test
	cd sistema-analise && ./mvnw -q test

.PHONY: demo
demo: ## Demo completa com pausas (requer serviços rodando)
	@./scripts/demo.sh

# ──────────────────────────────────────────────
#  Help
# ──────────────────────────────────────────────

.PHONY: help
help: ## Mostra esta ajuda
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | \
	  awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-18s\033[0m %s\n", $$1, $$2}'
