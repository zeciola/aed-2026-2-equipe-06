# Atividades — Gabriel Moreira da Silva de Faria (@gabezy)

**Matricula:** 1665580
**Tempo estimado:** ~1h20

---

## Tarefa 1: Tratar o retorno dos `send()` (ref: auditoria 4.6)

**Problema:** Quatro chamadas `send()` em `MargemService` e `AnaliseService` descartam o retorno. E o `EmprestimoService` trata mas esquece o `return` — loga "publicado com sucesso" mesmo quando falhou. O enunciado e explicito: "o retorno do `send()` tem dono" (B.2).

**O que fazer — 3 servicos:**

### 1.1. `sistema-emprestimo/.../service/EmprestimoService.java`

Adicionar o `return` que falta:

```diff
 kafkaTemplate.send(producerRecord)
         .whenComplete((records, throwable) -> {
             if (throwable != null) {
                 log.error("Erro ao publicar ...", throwable);
+                return;
             }
             log.info("Evento ... publicado com sucesso: {}", event);
         });
```

### 1.2. `sistema-margem/.../service/MargemService.java`

Criar metodo auxiliar e usar nas 2 publicacoes (reservada e recusada):

```java
private <T> void publicar(KafkaTemplate<String, T> template,
                           ProducerRecord<String, T> registro,
                           String tipo) {
    template.send(registro).whenComplete((resultado, erro) -> {
        if (erro != null) {
            log.error("Falha ao publicar {}: {}", tipo, erro.getMessage(), erro);
            return;
        }
        var meta = resultado.getRecordMetadata();
        log.info("{} publicado em {}-{}@{}", tipo,
                meta.topic(), meta.partition(), meta.offset());
    });
}
```

Substituir as chamadas diretas:

```diff
-        margemReservadaEventTemplate.send(reservadaEventProducerRecord);
+        publicar(margemReservadaEventTemplate, reservadaEventProducerRecord, "margem.reservada.v1");

-        margemRecusadaEventTemplate.send(recusadaEventProducerRecord);
+        publicar(margemRecusadaEventTemplate, recusadaEventProducerRecord, "margem.recusada.v1");
```

### 1.3. `sistema-analise/.../service/AnaliseService.java`

Mesmo padrao — criar o metodo `publicar` e usar nas 2 chamadas (aprovada e reprovada):

```diff
-        analiseAprovadaEventTemplate.send(aprovadaEventProducerRecord);
+        publicar(analiseAprovadaEventTemplate, aprovadaEventProducerRecord, "analise.aprovada.v1");

-        analiseReprovadaEventTemplate.send(reprovadaEventProducerRecord);
+        publicar(analiseReprovadaEventTemplate, reprovadaEventProducerRecord, "analise.reprovada.v1");
```

---

## Tarefa 2: Proteger `ce_id` nulo nos listeners (ref: auditoria 4.7)

**Problema:** `obterId()` loga o erro e devolve `null` quando nao acha `ce_id`. O `null` chega ao `INSERT INTO evento_processado`, viola a chave primaria, a excecao sobe, o `ack.acknowledge()` nunca roda, e o Kafka reentrega o registro para sempre — loop infinito.

**O que fazer — 2 arquivos:**

### 2.1. `sistema-margem/.../controller/MargemListener.java`

```diff
     public void verificarMargem(ConsumerRecord<String, EmprestimoSolicitadoEvent> consumerRecord,
                                 Acknowledgment ack) {
         var eventoId = obterId(consumerRecord);
+        if (eventoId == null || eventoId.isBlank()) {
+            log.error("Registro sem {} descartado: particao={} offset={}",
+                    CABECALHO_ID, consumerRecord.partition(), consumerRecord.offset());
+            ack.acknowledge();
+            return;
+        }
         margemService.processarSolicitacaoEmprestimo(eventoId, consumerRecord.value());
         ack.acknowledge();
     }
```

### 2.2. `sistema-analise/.../controller/AnaliseListener.java`

Mesma logica:

```diff
     public void analisarCredito(ConsumerRecord<String, MargemReservadaEvent> consumerRecord,
                                 Acknowledgment ack) {
         var eventoId = obterId(consumerRecord);
+        if (eventoId == null || eventoId.isBlank()) {
+            log.error("Registro sem {} descartado: particao={} offset={}",
+                    CABECALHO_ID, consumerRecord.partition(), consumerRecord.offset());
+            ack.acknowledge();
+            return;
+        }
         analiseService.processarAnaliseCredito(eventoId, consumerRecord.value());
         ack.acknowledge();
     }
```

**Conferir:**

```bash
cd sistema-margem && ./mvnw compile
cd sistema-analise && ./mvnw compile
```

Teste manual: publicar uma mensagem sem cabecalho pela Kafka UI — o listener deve logar o descarte e nao travar.

---

## Antes de commitar

```bash
git config user.name "1665580"
git config user.email "seu-email-do-github"
```
