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

- Docker e Docker Compose
- GNU Make
- Java 21 (apenas para desenvolvimento local e testes unitários)

No GitHub Codespaces o ambiente já vem pronto (ver `.devcontainer/`).

## Subir e validar — um único comando

```bash
make all
```

Esse target faz tudo automaticamente:

1. **Build** dos 3 serviços Java em containers Docker (multi-stage)
2. **Sobe** Kafka, Postgres, Kafka UI e os 3 serviços
3. **Aguarda** os healthchecks e o Spring Boot ficar pronto
4. **Executa a demo completa** — 3 cenários com explicações, consultas no banco e offsets Kafka

Cada passo explica o que está acontecendo: a cadeia de eventos, o que cada serviço faz,
e como a compensação funciona. Ao final, mostra o saldo de margem por CPF e a contagem
de eventos em cada tópico Kafka. O Kafka UI fica em `http://localhost:8089`.

Para derrubar: `make down` (ou `make down-clean` para apagar volumes).

## Todos os comandos

`make` sem argumentos mostra a ajuda:

```
  all                Sobe tudo, espera e executa a demo completa
  up                 Sobe só infraestrutura (Kafka, Postgres, Kafka UI)
  up-all             Sobe infraestrutura + os 3 serviços Java
  down               Derruba tudo
  down-clean         Derruba tudo e apaga volumes
  ps                 Estado dos containers
  rebuild            Rebuild dos serviços Java (infra mantida)
  logs               Logs dos 3 serviços (Ctrl+C sai)
  db                 Saldo de margem por CPF + últimos lançamentos
  test-aprovado      Fluxo feliz: análise aprova
  test-reprovado     Análise reprova → compensação (Saga)
  test-margem        Margem insuficiente
  test-dlq           Falha transitória → DLQ (~2 min)
  test-malformado    JSON malformado → DLQ imediata
  test               Testes unitários (precisa de Java 21)
  demo               Demo completa com pausas (requer serviços rodando)
```

## Cenários de teste

| Comando | Cenário | O que demonstra |
|---|---|---|
| `make test-aprovado` | CPF par → aprovado | Fluxo feliz completo |
| `make test-reprovado` | CPF ímpar → reprovado | **Saga**: margem reservada, depois compensada |
| `make test-margem` | Parcela > margem | Recusa sem acionar análise |
| `make test-dlq` | Banco indisponível simulado | **DLQ**: 9 tentativas com backoff → DLQ (~2 min) |
| `make test-malformado` | JSON inválido no tópico | DLQ imediata, sem retentativa |

## Demo

O script `scripts/demo.sh` percorre todos os cenários com pausas explicativas.
A demonstração do funcionamento foi gravada com [asciinema](https://asciinema.org/):

[![asciicast](https://asciinema.org/a/k9gSDkZW1FEvzXs3.svg)](https://asciinema.org/a/k9gSDkZW1FEvzXs3)

Para rodar localmente:

```bash
make up-all      # se ainda não estiver rodando
make demo        # executa a demo completa
```

## Modo desenvolvimento (serviços locais)

Para editar código com ciclo rápido:

```bash
make up    # sobe só Kafka, Postgres e Kafka UI

# Em 3 terminais separados:
cd sistema-emprestimo && ./mvnw spring-boot:run   # porta 8080
cd sistema-margem && ./mvnw spring-boot:run
cd sistema-analise && ./mvnw spring-boot:run
```

## Reprocessamento de DLQ

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
