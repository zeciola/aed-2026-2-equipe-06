# Atividades — Natan de Almeida Figueiredo (@natantn)

**Matricula:** 1669471
**Tempo estimado:** ~1h

---

## Tarefa 1: Corrigir emprestimo que aumenta a margem (ref: auditoria 4.8 / achado 2.5)

**Problema:** A lista `VERBAS_DEBITO = (600, 700, 800, 900)` decide se o lancamento e debito ou credito. Qualquer codigo de verba fora da lista entra como CREDITO e **soma** a margem disponivel. Um emprestimo com codigo errado passa a aumentar a capacidade de tomar emprestimo — bug grave de regra de negocio.

**O que fazer:**

Editar `sistema-margem/.../service/MargemService.java`:

```diff
-        var tipo = VERBAS_DEBITO.contains(event.codigoVerba())
-                ? Margem.Tipo.DEBITO : Margem.Tipo.CREDITO;
+        if (!VERBAS_DEBITO.contains(event.codigoVerba())) {
+            gerarMargemRecusadaEvent(eventoId, event.cpf(), "Codigo de verba nao permitido");
+            return;
+        }
+        var tipo = Margem.Tipo.DEBITO;
```

A logica e: uma solicitacao de emprestimo e **sempre debito**. Se o codigo de verba nao esta na lista permitida, recusa.

**Conferir:** Subir os servicos e enviar uma solicitacao com `codigoVerba: 999` — deve gerar `margem.recusada.v1`, nao reservar margem.

---

## Tarefa 2: Ajustar `MargemReservadaEvent` para usar `emprestimoId` correto (ref: auditoria 4.5 / achado 2.3)

**Problema:** O campo `solicitacaoId` do `MargemReservadaEvent` recebe o valor que veio como `ce_id` — que e o id do emprestimo, nao o id da solicitacao. Quando o `ce_id` virar UUID proprio (tarefa do Jose Ricardo), esse campo fica sem sentido.

**O que fazer:**

### 2.1. `sistema-margem/.../domain/MargemReservadaEvent.java`

Verificar que o campo se chama `emprestimoId` (ou renomear de `solicitacaoId` para `emprestimoId`) e que recebe o id correto vindo do evento de entrada.

### 2.2. `sistema-margem/.../service/MargemService.java`

Na chamada de `gerarMargemReservadaEvent`, passar `event.emprestimoId()` (o id do negocio que vem no corpo do evento) em vez do `eventoId` (que era o `ce_id` do cabecalho):

```diff
-    gerarMargemReservadaEvent(eventoId, event.cpf());
+    gerarMargemReservadaEvent(event.cpf(), event.emprestimoId());
```

### 2.3. `sistema-analise/.../domain/MargemReservadaEvent.java`

Verificar que este consumidor tambem declara `emprestimoId` (em vez de `solicitacaoId`) para manter consistencia.

> **Coordenacao:** essa tarefa depende da tarefa do Jose Ricardo (que adiciona `emprestimoId` ao evento do produtor). Combinar a ordem dos commits.

**Conferir:**

```bash
cd sistema-margem && ./mvnw compile
cd sistema-analise && ./mvnw compile
```

---

## Tarefa 3: Documentar configuracao de git com matricula (ref: auditoria 4.12)

**Problema:** Os commits estao nas contas pessoais. Nao ha conserto retroativo, mas daqui para frente cada integrante deve commitar com a matricula.

**O que fazer:**

Cada integrante deve rodar no seu repositorio local:

```bash
git config user.name "SUA_MATRICULA"
git config user.email "seu-email-do-github"
```

Incluir essa instrucao no README ou na folha de rosto (coordenar com Rodrigo).

Conferir com:

```bash
git log --format='%an <%ae>' -5
```

---

## Antes de commitar

```bash
git config user.name "1669471"
git config user.email "seu-email-do-github"
```
