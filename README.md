# aed-2026-2-equipe-06

Arquitetura Reativa e Event-Driven · Atividade Incremental · AED 2026/2 · Equipe 06

## Equipe

Líder: Gabriel Moreira da Silva de Faria

| Nome | Matrícula | Username |
| --- | --- | --- |
| Henrique Miguel de Jesus | 255486 | @HenriqueDest |
| Paulo Henrique Nunes Vanderley | 1456295 | @paulnune |
| Rodrigo Pretes Maia | 257199 | @RodrigoPretes-257199 |
| José Ricardo Ciola Bricio | 1669938 | @zeciola |
| Diego Bruno Dantas Diógenes | 1665455 | @diegodiogenes |
| Gabriel Moreira da Silva de Faria | 1665580 | @gabezy |
| Natan de Almeida Figueiredo | 1669471 | @natantn |

## O domínio, em uma frase

Solicitação de empréstimo consignado que reserva margem salarial e passa por análise de
crédito em sistemas externos, com compensação (cancelamento da reserva de margem) quando a
análise reprova. Detalhes e critérios atendidos em [docs/adr/ADR-002-dominio-do-projeto.md](docs/adr/ADR-002-dominio-do-projeto.md).

## Como rodar

Pré-requisitos: Java 21, Docker e Docker Compose. Cada serviço é um projeto Maven
independente (usa o próprio `./mvnw`, sem pom pai).

No GitHub Codespaces o ambiente já vem pronto (ver `.devcontainer/`).
Em Codespace antigo, sem rebuild: `sdk use java 21.0.10-ms`.

### Modo rápido — tudo de uma vez

```bash
docker compose -f compose.yml -f compose.services.yml up -d --build
```

Sobe infraestrutura (Kafka, Postgres, Kafka UI) e os três serviços Java em containers.
Útil para testar o fluxo completo ou avaliar o projeto sem abrir vários terminais.

### Modo desenvolvimento — serviços locais

Preferível quando se está editando código (ciclo mais rápido, log isolado por serviço).

**1. Subir a infraestrutura:**

```bash
docker compose up -d
```

Sobe Kafka (porta `19093`), Postgres (porta `15430`, banco `aed`) e o Kafka UI
(`http://localhost:8089`, pra inspecionar tópicos e mensagens pelo navegador).

**2. Subir os três serviços** (cada um em um terminal):

```bash
cd sistema-emprestimo && ./mvnw spring-boot:run   # publisher HTTP, porta 8080
```
```bash
cd sistema-margem && ./mvnw spring-boot:run       # consumidor idempotente (sem porta HTTP)
```
```bash
cd sistema-analise && ./mvnw spring-boot:run      # consumidor idempotente (sem porta HTTP)
```

### 3. Disparar uma solicitação de empréstimo

Requisições de exemplo já prontas em [`sistema-emprestimo/request/`](sistema-emprestimo/request/):

```bash
cd sistema-emprestimo/request
./make_request.sh emprestimo.json                  # fluxo feliz
./make_request.sh cpf_margem_insuficiente.json      # recusado por margem insuficiente
./make_request.sh cpf_analise_aprovada.json         # margem ok, análise de crédito aprova
./make_request.sh cpf_analise_reprovada.json        # margem ok, análise de crédito reprova
```

A API responde `202 Accepted` na hora — o resultado (margem reservada/recusada, crédito
aprovado/reprovado) acontece de forma assíncrona, via Kafka.

### 4. Conferir o resultado

Pelo banco:

```bash
docker exec postgres psql -U postgres -d aed -c "select * from emprestimo order by data_emprestimo desc limit 5;"
docker exec postgres psql -U postgres -d aed -c "select * from margem order by criado_em desc limit 5;"
docker exec postgres psql -U postgres -d aed -c "select * from analise order by criado_em desc limit 5;"
```

Pelo Kafka (cabeçalhos CloudEvents inclusos):

```bash
docker exec kafka kafka-console-consumer --bootstrap-server localhost:29092 \
  --topic analise.aprovada.v1 --property print.headers=true --from-beginning
```

Ou pelo Kafka UI em `http://localhost:8089`.

### 5. Rodar os testes automatizados

```bash
cd sistema-emprestimo && ./mvnw test
cd sistema-margem && ./mvnw test
cd sistema-analise && ./mvnw test
```

O `sistema-analise` tem um teste de idempotência ponta a ponta (`IdempotenciaTest`) que sobe
Kafka embutido e H2 em memória — roda sozinho, sem precisar do `docker compose up`.
