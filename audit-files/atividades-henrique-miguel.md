# Atividades — Henrique Miguel de Jesus (@HenriqueDest)

**Matricula:** 255486
**Tempo estimado:** ~30 min

---

## Tarefa 1: Criar e empurrar a tag `entrega-aula-02` (ref: auditoria 4.1)

**Problema:** `git ls-remote --tags origin` volta vazio. O enunciado exige a tag `entrega-aula-02`.

**O que fazer:**

```bash
git tag entrega-aula-02 e178316
git push origin entrega-aula-02

# conferir
git ls-remote --tags origin
# deve listar refs/tags/entrega-aula-02
```

> O commit `e178316` e o ultimo dentro do prazo (23/08 20:11). Os commits posteriores so ajustam usernames no README.

---

## Tarefa 2: Corrigir o healthcheck do Postgres (ref: auditoria 4.4)

**Problema:** O healthcheck faz `pg_isready -U postgres -d app`, mas o banco criado e `aed`. O container fica permanentemente *unhealthy*.

**O que fazer:**

Editar `compose.yml`, na secao do `postgres`:

```diff
   postgres:
     healthcheck:
-      test: ["CMD-SHELL", "pg_isready -U postgres -d app"]
+      test: ["CMD-SHELL", "pg_isready -U postgres -d aed"]
```

**Conferir:** `docker compose up -d && docker compose ps` — o Postgres deve ficar `healthy`.

---

## Tarefa 3: Renomear/eliminar o `TestPublisher` (ref: auditoria 4.10)

**Problema:** O sufixo "Publisher" nao esta na lista fechada de sufixos permitidos pelo enunciado, e e vizinho do "Producer" proibido (item 12 do checklist).

**O que fazer:**

Apagar o arquivo `sistema-analise/src/test/java/.../TestPublisher.java` e mover o metodo `publicar` para dentro do proprio `IdempotenciaTest.java` como metodo privado:

```diff
- // arquivo TestPublisher.java
- public class TestPublisher {
-     public void publicar(String eventoId, String cpf, String solicitacaoId) { ... }
- }

  // dentro de IdempotenciaTest.java:
+ private void publicar(String eventoId, String cpf, String emprestimoId) {
+     // conteudo que estava no TestPublisher
+ }
```

Atualizar os imports e referencias no `IdempotenciaTest` para usar o metodo local.

**Conferir:** `cd sistema-analise && ./mvnw test` — os testes devem continuar passando.

---

## Antes de commitar

```bash
git config user.name "255486"
git config user.email "seu-email-do-github"
```
