# CredFolha

Plataforma de crédito consignado com sagas coreografadas, idempotência e Apache Kafka.

Três serviços Maven independentes comunicam-se estritamente por eventos de domínio
(CloudEvents 1.0) no Apache Kafka, com consistência eventual, deduplicação atômica
e compensação automática.

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
análise reprova. Detalhes em [docs/adr/ADR-002-dominio-do-projeto.md](docs/adr/ADR-002-dominio-do-projeto.md).

## Pré-requisitos

- Java 21
- Docker e Docker Compose
- [asciinema](https://asciinema.org/) (opcional, para gravar a demo)
- GNU Make

No GitHub Codespaces o ambiente já vem pronto (ver `.devcontainer/`).

## Quick start

```bash
make up-all        # sobe Kafka, Postgres, Kafka UI e os 3 serviços Java
make test-aprovado # dispara o fluxo feliz
make db-margem     # verifica o resultado no banco
```

Para derrubar: `make down` (ou `make down-clean` para apagar volumes).

## Comandos disponíveis

`make` sem argumentos mostra a ajuda completa. Resumo:

### Infraestrutura

| Comando | O que faz |
|---|---|
| `make up` | Sobe infraestrutura (Kafka, Postgres, Kafka UI) |
| `make up-all` | Sobe infraestrutura + os 3 serviços Java em containers |
| `make down` | Derruba tudo |
| `make down-clean` | Derruba tudo e apaga volumes (banco e Kafka) |
| `make ps` | Estado dos containers |
| `make rebuild` | Rebuild e restart dos serviços Java (infra mantida) |

### Logs

| Comando | O que faz |
|---|---|
| `make logs` | Todos os serviços de domínio |
| `make logs-emprestimo` | Apenas sistema-emprestimo |
| `make logs-margem` | Apenas sistema-margem |
| `make logs-analise` | Apenas sistema-analise |

### Cenários de teste

| Comando | Cenário | O que demonstra |
|---|---|---|
| `make test-aprovado` | CPF par → aprovado | Fluxo feliz completo |
| `make test-reprovado` | CPF ímpar → reprovado | **Saga**: margem reservada, depois compensada |
| `make test-margem` | Parcela > margem | Recusa sem acionar análise |
| `make test-dlq` | Banco indisponível simulado | **DLQ**: 9 tentativas com backoff → DLQ (~2 min) |
| `make test-malformado` | JSON inválido no tópico | DLQ imediata, sem retentativa |

### Consultas no banco

| Comando | O que mostra |
|---|---|
| `make db-emprestimo` | Últimos empréstimos |
| `make db-margem` | Últimos lançamentos (débitos e créditos) |
| `make db-analise` | Últimas análises de crédito |
| `make db-saldo` | Saldo de margem por CPF |

### Testes unitários

```bash
make test    # roda os testes dos 3 serviços (precisa de Java 21, sem Docker)
```

## Demo gravada com asciinema

O script `scripts/demo.sh` percorre todos os cenários com pausas e explicações.

```bash
# Rodar interativamente (requer make up-all antes)
make demo

# Gravar com asciinema (gera demo.cast)
make demo-record

# Reproduzir a gravação
make demo-play
```

A gravação pode ser compartilhada com `asciinema upload demo.cast` ou embedada
no README com o player web.

## Modo desenvolvimento (serviços locais)

Para ciclo rápido de edição, suba só a infraestrutura e rode os serviços localmente:

```bash
make up    # sobe só Kafka, Postgres e Kafka UI

# Em 3 terminais separados:
cd sistema-emprestimo && ./mvnw spring-boot:run   # porta 8080
cd sistema-margem && ./mvnw spring-boot:run
cd sistema-analise && ./mvnw spring-boot:run
```

Os `make test-*` funcionam em ambos os modos.

## Verificar resultados

### Pelo banco

```bash
make db-emprestimo
make db-margem
make db-saldo       # saldo de margem por CPF (débitos - créditos)
make db-analise
```

### Pelo Kafka UI

Abra `http://localhost:8089` no navegador. Tópicos relevantes:

- `emprestimo.solicitado.v1` / `.dlq`
- `margem.reservada.v1` / `.dlq`
- `margem.recusada.v1`
- `margem.liberada.v1` (compensação)
- `analise.aprovada.v1`
- `analise.reprovada.v1`

### Pelo terminal Kafka

```bash
docker exec kafka kafka-console-consumer --bootstrap-server localhost:29092 \
  --topic analise.aprovada.v1 --property print.headers=true --from-beginning
```

## Cenário de reprocessamento (DLQ)

Após corrigir a causa raiz, republicar o evento da DLQ no tópico original:

```bash
docker exec kafka kafka-console-consumer --bootstrap-server localhost:29092 \
  --topic emprestimo.solicitado.v1.dlq --from-beginning --max-messages 1 \
  --property print.headers=true --property print.key=true \
  | docker exec -i kafka kafka-console-producer --bootstrap-server localhost:29092 \
  --topic emprestimo.solicitado.v1 --property parse.headers=true --property parse.key=true
```

A idempotência por `ce_id` garante que reprocessar o mesmo evento não duplica efeitos.

## Documentação

| Documento | Conteúdo |
|---|---|
| [docs/arquitetura.md](docs/arquitetura.md) | Documento de arquitetura (8 seções) |
| [docs/contrato.md](docs/contrato.md) | Contratos dos eventos de domínio |
| [docs/adr/ADR-002-dominio-do-projeto.md](docs/adr/ADR-002-dominio-do-projeto.md) | Domínio do projeto |
| [docs/adr/ADR-003-ambiente-de-execucao.md](docs/adr/ADR-003-ambiente-de-execucao.md) | Ambiente de execução |
| [docs/adr/ADR-006-resiliencia.md](docs/adr/ADR-006-resiliencia.md) | Saga, compensação e resiliência |
| [docs/IA.md](docs/IA.md) | Registro de uso de IA |
| [docs/apresentacao.pdf](docs/apresentacao.pdf) | Slides da apresentação |
