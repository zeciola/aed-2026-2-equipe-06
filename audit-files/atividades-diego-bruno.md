# Atividades — Diego Bruno Dantas Diogenes (@diegodiogenes)

**Matricula:** 1665455
**Tempo estimado:** ~1h30

---

## Tarefa 1: Criar `IdempotenciaTest` no `sistema-margem` (ref: auditoria 4.9)

**Problema:** O checklist item 05 (idempotencia) esta PARCIAL porque o `sistema-margem` so tem `contextLoads`. O enunciado pede que o consumidor da Parte B comprove que o mesmo evento 3x produz efeito 1x.

**O que fazer:**

### 1.1. Adicionar H2 como dependencia de teste

Editar `sistema-margem/pom.xml`:

```xml
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>test</scope>
</dependency>
```

### 1.2. Criar o teste de idempotencia

Criar `sistema-margem/src/test/java/br/com/puc/aed/sistemamargem/IdempotenciaTest.java`:

```java
@SpringBootTest
@EmbeddedKafka(partitions = 3, topics = {"emprestimo.solicitado.v1",
                                         "margem.reservada.v1",
                                         "margem.recusada.v1"})
@TestPropertySource(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.datasource.url=jdbc:h2:mem:aed;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.kafka.consumer.auto-offset-reset=earliest"
})
class IdempotenciaTest {

    // Injetar KafkaTemplate e JdbcTemplate

    @Test
    @DisplayName("o MESMO evento entregue tres vezes reserva margem uma vez so")
    void reentregaNaoDuplicaOEfeito() {
        String eventoId = UUID.randomUUID().toString();
        // publicar o evento 3 vezes com o mesmo eventoId
        // aguardar processamento
        // consultar banco e confirmar que so existe 1 margem
    }
}
```

> Use o `IdempotenciaTest` do `sistema-analise` como referencia — ele ja passa e tem a estrutura completa. Adapte para o contexto de margem.

> **Atencao:** se o H2 tropecar no `gen_random_uuid()` do seed, mova os `INSERT` do `schema.sql` para `data.sql` (coordenar com Rodrigo que faz a tarefa 2.8).

---

## Tarefa 2: Corrigir/apagar `contextLoads` nos tres modulos (ref: auditoria 4.9)

**Problema:** `SistemaAnaliseApplicationTests.contextLoads` derruba o build com `BUILD FAILURE` porque tenta subir o contexto real contra Postgres em `localhost:15430` que nao esta de pe. Acontece nos tres modulos.

**O que fazer:**

Apagar os arquivos de teste que so tem `contextLoads`:

- `sistema-analise/src/test/java/.../SistemaAnaliseApplicationTests.java`
- `sistema-margem/src/test/java/.../SistemaMargemApplicationTests.java`
- `sistema-emprestimo/src/test/java/.../SistemaEmprestimoApplicationTests.java`

Esses testes foram gerados pelo Spring Initializr e nao agregam valor — o contexto ja e exercitado pelos testes de idempotencia.

### 1.3. Adicionar H2 no `sistema-emprestimo` tambem

Editar `sistema-emprestimo/pom.xml`:

```xml
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>test</scope>
</dependency>
```

**Conferir:**

```bash
cd sistema-emprestimo && ./mvnw test
cd sistema-margem && ./mvnw test
cd sistema-analise && ./mvnw test
```

Todos devem terminar com `BUILD SUCCESS`.

---

## Antes de commitar

```bash
git config user.name "1665455"
git config user.email "seu-email-do-github"
```
