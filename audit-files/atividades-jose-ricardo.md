# Atividades — Jose Ricardo Ciola Bricio (@zeciola)

**Matricula:** 1669938
**Tempo estimado:** ~1h

---

## Tarefa 1: Dar identidade propria ao evento e consumidor tolerante (ref: auditoria 4.5)

**Problema triplo:**
1. O `ce_id` usa `emprestimo.getId()` (id da entidade, nao do fato) — exatamente o erro que a secao 9 do enunciado nomeia
2. O `sistema-margem` republica o mesmo `ce_id` em `margem.reservada.v1` — dois eventos diferentes, um id so
3. O consumidor declara exatamente os mesmos campos que o produtor publica, entao a tolerancia (`@JsonIgnoreProperties`) nunca e exercida

**O que fazer — 4 arquivos:**

### 1.1. `sistema-emprestimo/.../domain/EmprestimoSolicitadoEvent.java`

Adicionar campos `emprestimoId` e `valorTotal`:

```diff
 public final class EmprestimoSolicitadoEvent {

+    private final String emprestimoId;
     private final String cpf;
     private final BigDecimal valorParcela;
+    private final BigDecimal valorTotal;
     private final Integer codigoVerba;
```

Atualizar o construtor e os getters correspondentes.

### 1.2. `sistema-emprestimo/.../service/EmprestimoService.java`

Usar `UUID.randomUUID()` como `ce_id` e passar os novos campos:

```diff
         var event = new EmprestimoSolicitadoEvent(
+                emprestimo.getId().toString(),
                 emprestimo.getCpf(),
                 emprestimo.getValorParcela(),
+                emprestimo.getValorTotal(),
                 emprestimo.getCodigoVerba()
         );
 ...
-        producerRecord.headers().add("ce_id",
-                emprestimo.getId().toString().getBytes(StandardCharsets.UTF_8));
+        var eventoId = UUID.randomUUID().toString();
+        producerRecord.headers().add("ce_id", eventoId.getBytes(StandardCharsets.UTF_8));
```

Adicionar `import java.util.UUID;` se nao existir.

### 1.3. `sistema-margem/.../domain/EmprestimoSolicitadoEvent.java`

O consumidor NAO declara `valorTotal` — isso demonstra a tolerancia:

```diff
 @JsonIgnoreProperties(ignoreUnknown = true)
 public final class EmprestimoSolicitadoEvent {

+    private final String emprestimoId;
     private final String cpf;
     private final BigDecimal valorParcela;
     private final Integer codigoVerba;
+    // valorTotal NAO e declarado de proposito:
+    // este consumidor nao precisa desse campo, demonstrando tolerancia
```

Atualizar construtor e getters para incluir `emprestimoId` mas NAO `valorTotal`.

### 1.4. `sistema-margem/.../service/MargemService.java`

UUID novo para cada publicacao do `sistema-margem`:

```diff
-    private void gerarMargemReservadaEvent(String eventoId, String cpf) {
-        var event = new MargemReservadaEvent(cpf, eventoId);
+    private void gerarMargemReservadaEvent(String cpf, String emprestimoId) {
+        var novoEventoId = UUID.randomUUID().toString();
+        var event = new MargemReservadaEvent(cpf, emprestimoId);
 ...
-        record.headers().add("ce_id", eventoId.getBytes(StandardCharsets.UTF_8));
+        record.headers().add("ce_id", novoEventoId.getBytes(StandardCharsets.UTF_8));
```

Fazer o mesmo para `gerarMargemRecusadaEvent`.

> **Efeito colateral positivo:** com `ce_id` proprio, a tabela `evento_processado_analise` pode voltar a se chamar `evento_processado` — os ids nao colidem mais.

**Conferir:**

```bash
cd sistema-emprestimo && ./mvnw compile
cd sistema-margem && ./mvnw compile
```

Subir com `docker compose up -d` e disparar uma solicitacao. Verificar no Kafka UI que o `ce_id` e um UUID diferente em cada topico.

---

## Antes de commitar

```bash
git config user.name "1669938"
git config user.email "seu-email-do-github"
```
