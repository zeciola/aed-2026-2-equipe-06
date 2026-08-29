# Aula 02 — Folha de rosto

## O que foi feito nesta etapa

- **Parte A** — domínio escolhido e registrado em [ADR-002](../adr/ADR-002-dominio-do-projeto.md):
  empréstimo consignado, com reserva de margem, análise de crédito e compensação
  por cancelamento de margem.
- **Parte B** — três serviços Maven independentes comunicando-se só por eventos:
  - `sistema-emprestimo` — publisher HTTP (202 Accepted) → `emprestimo.solicitado.v1`
  - `sistema-margem` — consumidor idempotente → `margem.reservada.v1` / `margem.recusada.v1`
  - `sistema-analise` — consumidor idempotente → `analise.aprovada.v1` / `analise.reprovada.v1`
- **Registro de IA** em [docs/IA.md](../IA.md), com quatro recusas justificadas.

## Por onde começar a leitura

1. `sistema-margem/.../service/MargemService.java` — dedup e efeito no mesmo `@Transactional`
2. `sistema-margem/.../controller/MargemListener.java` — o `ack` depois do commit
3. `sistema-analise/src/test/.../IdempotenciaTest.java` — o mesmo evento 3x, efeito 1x

## Como rodar

    docker compose up -d
    cd sistema-emprestimo && ./mvnw spring-boot:run   # em um terminal
    cd sistema-margem     && ./mvnw spring-boot:run   # em outro
    cd sistema-analise    && ./mvnw spring-boot:run   # em outro
    cd sistema-emprestimo/request && ./make_request.sh emprestimo.json

## Quem fez o que

| Parte | Quem |
| --- | --- |
| ADR-002 — domínio | Gabriel Moreira (trouxe o domínio), Rodrigo Maia (redação) |
| `sistema-emprestimo` | Gabriel Moreira (estrutura inicial), Rodrigo Maia (ajustes de pom.xml), Paulo Henrique (cabeçalhos `ce_`) |
| `sistema-margem` | Gabriel Moreira (estrutura inicial e eventos), Rodrigo Maia (ajustes de pom.xml), Paulo Henrique (cabeçalhos `ce_`) |
| `sistema-analise` | Rodrigo Maia (estrutura inicial e schema), Paulo Henrique (cabeçalhos `ce_`) |
| `IA.md` / revisão | Rodrigo Maia (redação e revisão), Gabriel Moreira (ajustes de cabeçalho) |

## Observação sobre as contas do GitHub

Os commits estão nas contas pessoais de cada integrante; a tabela de
matriculas esta no [README](../../README.md#equipe).
