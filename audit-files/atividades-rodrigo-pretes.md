# Atividades — Rodrigo Pretes Maia (@RodrigoPretes-257199)

**Matricula:** 257199
**Tempo estimado:** ~40 min

---

## Tarefa 1: Reescrever a folha de rosto `docs/entregas/aula-02.md` (ref: auditoria 4.11)

**Problema:** A folha de rosto e o primeiro arquivo que o professor abre, e hoje ela diz que a Parte B nao foi feita ("pendente"). Isso e o item 02 da ordem de ataque.

**O que fazer:**

Substituir o conteudo de `docs/entregas/aula-02.md` por:

```markdown
# Aula 02 — Folha de rosto

## O que foi feito nesta etapa

- **Parte A** — dominio escolhido e registrado em [ADR-002](../adr/ADR-002-dominio-do-projeto.md):
  emprestimo consignado, com reserva de margem, analise de credito e compensacao
  por cancelamento de margem.
- **Parte B** — tres servicos Maven independentes comunicando-se so por eventos:
  - `sistema-emprestimo` — publisher HTTP (202 Accepted) -> `emprestimo.solicitado.v1`
  - `sistema-margem` — consumidor idempotente -> `margem.reservada.v1` / `margem.recusada.v1`
  - `sistema-analise` — consumidor idempotente -> `analise.aprovada.v1` / `analise.reprovada.v1`
- **Registro de IA** em [docs/IA.md](../IA.md), com quatro recusas justificadas.

## Por onde comecar a leitura

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
| ADR-002 — dominio | Gabriel Moreira (trouxe o dominio), Rodrigo Maia (redacao) |
| `sistema-emprestimo` | (preencher) |
| `sistema-margem` | (preencher) |
| `sistema-analise` | (preencher) |
| `IA.md` / revisao | (preencher) |

## Observacao sobre as contas do GitHub

Os commits estao nas contas pessoais de cada integrante; a tabela de
matriculas esta no [README](../../README.md#equipe).
```

> **IMPORTANTE:** Preencha a tabela "Quem fez o que" com os nomes reais de quem trabalhou em cada parte. Consulte o `git log` e os colegas.

---

## Tarefa 2: Mover dados de seed de `schema.sql` para `data.sql` (ref: auditoria 2.8)

**Problema:** O `sistema-margem/src/main/resources/schema.sql` contem `DELETE` + `INSERT` de dados de exemplo (quatro CPFs de credito). Dados de seed pertencem a `data.sql`, nao a `schema.sql`.

**O que fazer:**

1. Criar `sistema-margem/src/main/resources/data.sql`
2. Mover os comandos `DELETE FROM ...` e `INSERT INTO ...` do `schema.sql` para `data.sql`
3. Deixar no `schema.sql` apenas o DDL (`CREATE TABLE`, `CREATE INDEX` etc.)

**Conferir:** Subir o `sistema-margem` com `docker compose up -d && cd sistema-margem && ./mvnw spring-boot:run` — os dados de seed devem aparecer no banco normalmente.

---

## Antes de commitar

```bash
git config user.name "257199"
git config user.email "seu-email-do-github"
```
