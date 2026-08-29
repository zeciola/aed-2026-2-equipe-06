# Auditoria do repositório da Equipe 06

**AED · Arquitetura Reativa e Event-Driven · Unidade II · Etapa 1 · revisão pré-correção**

| | |
| --- | --- |
| Repositório | `zeciola/aed-2026-2-equipe-06` |
| Commit auditado | `ccce7b7` (árvore de trabalho limpa) |
| Escopo | 3 serviços · 7 eventos · 5 tópicos |
| Build | executado no Codespace da equipe com Java 21 |

A idempotência é real — o teste roda e passa. A estrutura está no lugar e o ADR é sólido.
O que custa nota está quase todo na borda: um caractere no cabeçalho, uma tag que não
existe, uma folha de rosto que ainda diz "pendente", e um build que não sobe numa
máquina limpa.

| Itens OK | Itens falhando | Bugs de código |
| --- | --- | --- |
| 14 | 6 | 8 |

---

## 1. Checklist da seção 5 do enunciado

| # | Verificação | Status |
| --- | --- | --- |
| 01 | Nome no particípio, descrevendo fato ocorrido — Solicitado, Reservada, Recusada, Aprovada, Reprovada | **passa** |
| 02 | Nenhum nome de evento com raiz CRUD | **passa** |
| 03 | Os quatro atributos `ce_*` obrigatórios nos cabeçalhos — presentes, mas grafados com **hífen** (`ce-id`, não `ce_id`). 32 ocorrências | **FALHA** |
| 04 | Nenhuma data em epoch — `Instant.toString()` em todos os `ce-time` | **passa** |
| 05 | Mesmo evento 3× produz efeito 1× — comprovado no `sistema-analise`; `sistema-margem`, que é o consumidor da Parte B, só tem `contextLoads` | **PARCIAL** |
| 06 | Offset confirmado depois do efeito — `ack.acknowledge()` é a última linha dos dois listeners, `manual_immediate` | **passa** |
| 07 | ADR-002 com "Consequências aceitas" preenchida — cinco seções, e a última antecipa a aula 05 nominalmente | **passa** |
| 08 | IA.md com pelo menos uma recusa justificada — quatro recusas, com razão técnica em cada | **passa** |
| 09 | Nenhum dado pessoal real — CPFs de teste, nenhum e-mail ou telefone | **passa** |
| 10 | Compila e sobe numa máquina limpa — compila com Java 21, mas `mvn test` termina em **BUILD FAILURE** (8 testes, 1 erro: `contextLoads` não sobe sem Postgres e Kafka). E o Codespace vem com Java 11, então sem ajuste manual nem compila | **FALHA** |
| 11 | Quatro pacotes com os nomes do padrão — raiz / controller / domain / service, nos três serviços | **passa** |
| 12 | Todo sufixo na lista fechada — `TestPublisher`: "Publisher" não está na lista, e é vizinho do "Producer" proibido | **FALHA** |
| 13 | `domain` não importa framework de infraestrutura — só Jackson e a biblioteca padrão | **passa** |
| 14 | `@Transactional` só em `Service` | **passa** |
| 15 | Repositório abre sem login — clonado anonimamente | **passa** |
| 16 | Os quatro arquivos nos caminhos exatos | **passa** |
| 17 | Cada integrante com commit na própria conta — sete usernames, nenhum é a matrícula; um integrante com duas grafias de nome; um com só um commit, de README | **FALHA** |
| 18 | Tag `entrega-aula-02` no remoto — `git ls-remote --tags origin` volta vazio | **FALHA** |
| 19 | ADR-002 commitado antes do código — ADR em 17/08, primeiro `.java` em 18/08 | **passa** |
| 20 | No Canvas, só o documento de identificação — uma página com integrantes, líder e URL | **passa** |

### Resultado real dos testes (Codespace, Java 21)

```
IdempotenciaTest ............ 2 testes  OK   (16s, Kafka embutido)
AnaliseServiceTest .......... 3 testes  OK
AnaliseTest ................. 2 testes  OK
SistemaAnaliseApplicationTests.contextLoads  ERRO
Tests run: 8, Failures: 0, Errors: 1  ->  BUILD FAILURE
```

Causa do erro: `IllegalStateException: Failed to load ApplicationContext` — o teste tenta
subir o contexto real contra Postgres em `localhost:15430`, que não está de pé.

---

## 2. O que o checklist não pega

### 2.1 · O repositório não declara a própria versão de Java — CRÍTICO

`raiz do repo · sem .devcontainer/` — reprodutibilidade

Verificado no Codespace da equipe: a imagem padrão traz **Java 11**, e o `./mvnw test`
morre no primeiro `maven-compiler-plugin:compile` — nenhum dos três serviços chega a
compilar. Java 21 existe na máquina (via SDKMAN, `21.0.10-ms`), mas ninguém é avisado
disso: o README diz apenas "pré-requisitos: Java 21", e o repositório não carrega nada
que configure isso sozinho.

Um Codespace novo é literalmente a "máquina limpa" do item 10. Se o professor abrir o
projeto assim — e é o caminho de menor esforço para ele — o primeiro comando do README
falha.

Com Java 21 forçado na mão, o quadro melhora bastante: compila, e o `IdempotenciaTest`
passa de verdade. O que ainda derruba o build é o `contextLoads`.

> **Correção:** um `.devcontainer/devcontainer.json` com a feature `java:21` resolve para
> todo mundo e para sempre. Enquanto isso não existe, vale uma linha no README:
> `sdk use java 21.0.10-ms`.

### 2.2 · Um cabeçalho ausente derruba o consumidor em loop infinito — CRÍTICO

`MargemListener.java · AnaliseListener.java` — idempotência

`obterId()` loga o erro e devolve `null` quando não acha `ce-id` — mas o listener segue
em frente. O `null` chega ao `INSERT INTO evento_processado`, viola a chave primária, a
exceção sobe, o `ack.acknowledge()` nunca roda, e o Kafka reentrega o mesmo registro para
sempre.

É o cenário mais fácil de reproduzir na correção: basta publicar uma mensagem sem
cabeçalho pela Kafka UI.

> **Correção:** falhar cedo no listener — se o cabeçalho não vier, lançar exceção de
> contrato e mandar para DLQ, ou dar `ack` e descartar explicitamente. As duas são
> defensáveis; seguir em frente com `null` não é.

### 2.3 · O `ce_id` não é o id do evento — e viaja de um evento para o outro — CRÍTICO

`EmprestimoService · MargemService` — contrato no fio · idempotência

O publisher manda `emprestimo.getId()` como `ce-id`: é o id da entidade, exatamente o
erro que a seção 9 do enunciado nomeia. Pior, o `sistema-margem` pega esse mesmo id e o
republica como `ce-id` de `margem.reservada.v1` — dois eventos diferentes, um id só.

Foi por isso que a tabela de dedup precisou de nome próprio (`evento_processado_analise`):
com o nome padrão, o `sistema-analise` nunca processaria nada, porque o id já estaria
gravado. O `IA.md` registra isso como escolha consciente, mas é um curativo.

O mesmo id ainda vaza para o campo `solicitacaoId` do `MargemReservadaEvent`, que grava em
`analise.solicitacao_id` algo que não é o id da solicitação. Hoje casa com `emprestimo.id`
por acidente; quebra no instante em que o `ce_id` virar um UUID próprio.

> **Correção:** `UUID.randomUUID()` a cada publicação, como o `sistema-analise` já faz.
> E passar o `emprestimo.id` de verdade no `solicitacaoId`.

### 2.4 · Marca como processado antes de publicar, e ignora se a publicação falhou — CRÍTICO

`MargemService · AnaliseService` — idempotência

`registrarSeNovo()` é a primeira linha do método. Depois vem o efeito, e só então o
`send()` — que é assíncrono e, ao contrário do publisher, tem o retorno jogado fora nas
quatro chamadas. Se o broker recusar, o evento já está marcado como processado e a
reentrega será descartada em silêncio: o efeito aconteceu, o fato nunca foi publicado.

O enunciado é explícito em B.2: "o retorno do `send()` tem dono".

> **Correção:** tratar o retorno nos quatro `send()`. Publicar fora da transação (após o
> commit) é a versão certa, mas dá para argumentar o escopo desta etapa.

### 2.5 · Um empréstimo com o código de verba errado aumenta a margem do cliente

`MargemService.java · Margem.java` — regra de negócio

A lista `VERBAS_DEBITO = (600, 700, 800, 900)` decide se o lançamento é débito ou crédito,
e o construtor de `Margem` inverte o sinal do valor a partir dela. Qualquer código fora da
lista — inclusive um que o cliente escolha no corpo do `POST` — entra como CRÉDITO e
*soma* à margem disponível. Um empréstimo passa a aumentar a capacidade de tomar
empréstimo.

> **Correção:** uma solicitação de empréstimo é sempre débito. Se o código de verba precisa
> existir no evento, ele deve ser validado contra uma lista permitida, e não usado para
> inferir o sinal.

### 2.6 · O consumidor tolerante está anotado, mas não demonstrado

`EmprestimoSolicitadoEvent · MargemReservadaEvent` — contrato no fio

Os dois lados têm `@JsonIgnoreProperties(ignoreUnknown = true)` — e declaram exatamente os
mesmos campos que o produtor publica. Não há campo desconhecido para ignorar, então a
tolerância nunca é exercida. O enunciado pede o contrário, textualmente: "declare menos
campos do que o produtor publica, de propósito".

É a diferença entre "Adequado" e "Exemplar" num critério que vale 20%.

> **Correção:** publicar `valorTotal` (ou `eventoId`) no `EmprestimoSolicitadoEvent` e
> deixar o consumidor sem ele — com um teste que prove que a mensagem continua
> desserializando.

### 2.7 · O healthcheck do Postgres pergunta por um banco que não existe

`compose.yml` — infra

`pg_isready -U postgres -d app`, mas o banco criado é `aed`. O container sobe e funciona,
e fica permanentemente *unhealthy*. Custa zero para arrumar e é a primeira coisa que
aparece num `docker compose ps` durante a demonstração.

> **Correção:** trocar `-d app` por `-d aed`.

### 2.8 · Dados de exemplo dentro do `schema.sql` — menor

`sistema-margem/schema.sql`

O `DELETE` + `INSERT` dos quatro CPFs de crédito roda a cada subida do serviço, junto com o
DDL. Funciona, e o `DELETE` mantém a coisa idempotente — mas seed de dados é `data.sql`,
não `schema.sql`. É leitura, não nota.

### Fora de escopo, e corretamente

`margem.recusada.v1` e `analise.reprovada.v1` são publicados e ninguém consome. A
compensação — cancelar a reserva quando a análise reprova — é trabalho da aula 05, e tanto
o ADR-002 quanto o `IA.md` já registram isso por escrito. Não conte como pendência desta
etapa.

---

## 3. Ordem de ataque

| # | Tarefa | Custo |
| --- | --- | --- |
| 01 | Criar e empurrar a tag `entrega-aula-02` no commit do prazo | 2 min |
| 02 | Reescrever `docs/entregas/aula-02.md` — ela ainda diz "Parte B: pendente" | 20 min |
| 03 | Trocar `ce-` por `ce_` nos cinco cabeçalhos, nos três serviços e no `TestPublisher` | 10 min |
| 04 | Corrigir o healthcheck do Postgres para o banco `aed` | 1 min |
| 05 | Adicionar `.devcontainer/devcontainer.json` com Java 21 | 10 min |
| 06 | Dar identidade própria ao evento: `UUID` novo por publicação, e `solicitacaoId` com o id do empréstimo | 1 h |
| 07 | Copiar o `IdempotenciaTest` para o `sistema-margem` | 1 h |
| 08 | Tratar o retorno dos quatro `send()` e proteger o `ce-id` nulo nos listeners | 40 min |
| 09 | Publicar um campo a mais do que o consumidor declara | 30 min |
| 10 | Fazer os `contextLoads` pararem de exigir Postgres e Kafka reais | 30 min |
| 11 | Renomear/eliminar o `TestPublisher` | 5 min |

**Os usernames (item 17) não têm conserto retroativo.** O enunciado pede conta cujo login
é a matrícula, e os commits já estão feitos com contas pessoais. Dá para adicionar a
matrícula ao nome de exibição e manter a tabela do README como tradução — e vale mencionar
isso na folha de rosto, em vez de deixar o professor descobrir sozinho na aba Contributors.

---

## 4. Como fazer

Os trechos são para colar. Os caminhos estão abreviados só no rótulo; o pacote completo é
`br.com.puc.aed.<servico>`.

### 4.1 · A tag da entrega

Ela aponta para o commit que existia no prazo, e é esse que será corrigido. **Atenção ao
alvo:** `HEAD` é de 24/08, depois das 23h59 de domingo. O último commit dentro do prazo é
`e178316` (23/08 20:11).

```bash
git tag entrega-aula-02 e178316
git push origin entrega-aula-02

# confere - tem que listar refs/tags/entrega-aula-02
git ls-remote --tags origin
```

> **Decisão de vocês:** os dois commits de 24/08 só ajustam usernames no README. Marcar
> `e178316` é o literal do enunciado; marcar `ccce7b7` entrega um README mais completo, mas
> é um commit fora do prazo. A leitura conservadora é marcar `e178316`.

### 4.2 · O Codespace com Java 21

Sem isto, o primeiro comando do README falha numa máquina limpa. O arquivo não existe hoje;
crie a pasta.

**criar** — `.devcontainer/devcontainer.json`

```json
{
  "name": "aed-2026-2-equipe-06",
  "image": "mcr.microsoft.com/devcontainers/java:1-21-bookworm",
  "features": {
    "ghcr.io/devcontainers/features/docker-in-docker:2": {}
  },
  "forwardPorts": [8080, 8089, 15430, 19093],
  "postCreateCommand": "java -version"
}
```

**editar** — `README.md`, seção "Como rodar"

```markdown
## Como rodar

Pré-requisitos: Java 21, Docker e Docker Compose.
No GitHub Codespaces o ambiente já vem pronto (ver `.devcontainer/`).
Em Codespace antigo, sem rebuild: `sdk use java 21.0.10-ms`.
```

### 4.3 · Os cabeçalhos com underscore

32 ocorrências, três serviços mais o publicador de teste. Um `sed` resolve, e a conferência
é a mesma que a correção vai fazer.

```bash
grep -rl '"ce-' --include=*.java . \
  | xargs sed -i 's/"ce-\(specversion\|id\|source\|type\|time\)"/"ce_\1"/g'

# confere - nenhum hifen pode sobrar
grep -rho '"ce[-_][a-z]*"' --include=*.java . | sort | uniq -c
# esperado: 8 "ce_id"  6 "ce_source"  6 "ce_specversion"  6 "ce_time"  6 "ce_type"
```

### 4.4 · O healthcheck do Postgres

**editar** — `compose.yml`

```diff
   postgres:
     healthcheck:
-      test: ["CMD-SHELL", "pg_isready -U postgres -d app"]
+      test: ["CMD-SHELL", "pg_isready -U postgres -d aed"]
```

### 4.5 · Identidade do evento, id do negócio e consumidor tolerante — de uma vez

Três achados com uma mudança só. O evento passa a carregar o `emprestimoId` (identidade do
negócio) e o `valorTotal`; o `ce_id` vira um UUID novo por publicação (identidade do fato);
e o consumidor declara **um campo a menos**, que é a tolerância que o enunciado pede
demonstrada de verdade.

**editar** — `sistema-emprestimo/.../domain/EmprestimoSolicitadoEvent.java`

```diff
 public final class EmprestimoSolicitadoEvent {

+    private final String emprestimoId;
     private final String cpf;
     private final BigDecimal valorParcela;
+    private final BigDecimal valorTotal;
     private final Integer codigoVerba;
```

**editar** — `sistema-emprestimo/.../service/EmprestimoService.java`

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
+        // identidade do FATO, distinta da identidade do NEGOCIO
+        var eventoId = UUID.randomUUID().toString();
+        producerRecord.headers().add("ce_id", eventoId.getBytes(StandardCharsets.UTF_8));
```

**editar** — `sistema-margem/.../domain/EmprestimoSolicitadoEvent.java`

```diff
 @JsonIgnoreProperties(ignoreUnknown = true)
 public final class EmprestimoSolicitadoEvent {

+    private final String emprestimoId;
     private final String cpf;
     private final BigDecimal valorParcela;
     private final Integer codigoVerba;
+    // valorTotal NAO e declarado de proposito:
+    // este consumidor nao decide sobre dinheiro total, so sobre margem.
```

**editar** — `sistema-margem/.../service/MargemService.java`

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

> **Efeito colateral bom:** com o `ce_id` próprio, a tabela `evento_processado_analise`
> deixa de precisar do nome diferente — dá para voltar as duas para `evento_processado` e
> apagar o parágrafo de dívida técnica do `AGENTS.md`.

### 4.6 · O retorno do send(), com dono

Quatro chamadas em `MargemService` e `AnaliseService` descartam o retorno. E o
`EmprestimoService`, que trata, esquece o `return`: hoje ele loga "publicado com sucesso"
mesmo quando falhou.

**editar** — `sistema-emprestimo/.../service/EmprestimoService.java`

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

**editar** — `sistema-margem/.../service/MargemService.java` e `sistema-analise/.../service/AnaliseService.java`

```diff
+    private <T> void publicar(KafkaTemplate<String, T> template,
+                              ProducerRecord<String, T> registro,
+                              String tipo) {
+        template.send(registro).whenComplete((resultado, erro) -> {
+            if (erro != null) {
+                log.error("Falha ao publicar {}: {}", tipo, erro.getMessage(), erro);
+                return;
+            }
+            var meta = resultado.getRecordMetadata();
+            log.info("{} publicado em {}-{}@{}", tipo,
+                    meta.topic(), meta.partition(), meta.offset());
+        });
+    }

// e nos quatro pontos de publicacao:
-        margemReservadaEventTemplate.send(reservadaEventProducerRecord);
+        publicar(margemReservadaEventTemplate, reservadaEventProducerRecord, "margem.reservada.v1");
```

### 4.7 · O evento sem ce_id não pode derrubar o consumidor

Hoje `obterId()` devolve `null` e o fluxo continua: o `INSERT` viola a chave, a exceção
sobe, o `ack` nunca roda e o Kafka reentrega o mesmo registro para sempre. Vale para os dois
listeners.

**editar** — `sistema-margem/.../controller/MargemListener.java` e `sistema-analise/.../controller/AnaliseListener.java`

```diff
     public void verificarMargem(ConsumerRecord<String, EmprestimoSolicitadoEvent> consumerRecord,
                                 Acknowledgment ack) {
         var eventoId = obterId(consumerRecord);
+        if (eventoId == null || eventoId.isBlank()) {
+            log.error("Registro sem {} descartado: particao={} offset={}",
+                    CABECALHO_ID, consumerRecord.partition(), consumerRecord.offset());
+            ack.acknowledge();   // descarte explicito: sem id nao ha deduplicacao possivel
+            return;
+        }
         margemService.processarSolicitacaoEmprestimo(eventoId, consumerRecord.value());
         ack.acknowledge();
     }
```

### 4.8 · O empréstimo que aumentava a margem

O código de verba decide o sinal do lançamento, e ele vem no corpo do `POST`.

**editar** — `sistema-margem/.../service/MargemService.java`

```diff
-        var tipo = VERBAS_DEBITO.contains(event.codigoVerba())
-                ? Margem.Tipo.DEBITO : Margem.Tipo.CREDITO;
+        // O codigo de verba identifica a rubrica; ele nao decide o sinal.
+        if (!VERBAS_DEBITO.contains(event.codigoVerba())) {
+            gerarMargemRecusadaEvent(eventoId, event.cpf(), "Codigo de verba nao permitido");
+            return;
+        }
+        var tipo = Margem.Tipo.DEBITO;
```

### 4.9 · O teste que derruba o build, e o que falta no sistema-margem

`contextLoads` é o teste que o Initializr gerou: sobe o contexto real contra Postgres e
Kafka que não estão de pé. Nos três módulos. A correção que também resolve o item 5 do
checklist é substituí-lo, no `sistema-margem`, pelo teste de idempotência que o enunciado
pede — espelhando o do `sistema-analise`, que já passa.

**criar** — `sistema-margem/src/test/java/.../IdempotenciaTest.java`

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

    @Test
    @DisplayName("o MESMO evento entregue tres vezes reserva margem uma vez so")
    void reentregaNaoDuplicaOEfeito() {
        String eventoId = UUID.randomUUID().toString();
        publicador.publicar(eventoId, "22222222222", ...);
        aguardarQuantidadeDeMargens(1);
        publicador.publicar(eventoId, "22222222222", ...);
        publicador.publicar(eventoId, "22222222222", ...);
        confirmarQueQuantidadeNaoMuda(1);
    }
}
```

**editar** — `sistema-margem/pom.xml` e `sistema-emprestimo/pom.xml`

```xml
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>test</scope>
</dependency>
```

> **Dois detalhes:** apaguem `SistemaMargemApplicationTests` e
> `SistemaAnaliseApplicationTests` — o contexto já é exercitado pelos testes de verdade, e é
> só isso que derruba o build hoje. E se o H2 tropeçar no `gen_random_uuid()` do seed, movam
> os `INSERT` do `schema.sql` para um `data.sql` — que é a correção do achado 2.8 de
> qualquer jeito.

### 4.10 · O TestPublisher

"Publisher" não está na lista fechada de sufixos, e é vizinho do "Producer" proibido. A
saída mais limpa é não ter a classe: o publicador é usado por um teste só, então vira método
privado dele.

**apagar** — `sistema-analise/src/test/java/.../TestPublisher.java`

```diff
-public class TestPublisher {
-    public void publicar(String eventoId, String cpf, String solicitacaoId) { ... }
-}

// dentro do proprio IdempotenciaTest:
+    private void publicar(String eventoId, String cpf, String emprestimoId) {
+        ...
+    }
```

> **Alternativa válida:** o enunciado aceita sufixo fora da lista "com justificativa em
> ADR". Um `ADR-003` de meia página sobre nomenclatura de apoio a teste também fecha o item
> — e mostra que a decisão foi consciente.

### 4.11 · A folha de rosto

É o primeiro arquivo que o professor abre, e hoje ela diz que a Parte B não foi feita. O
texto abaixo é para colar por cima do que está lá.

**reescrever** — `docs/entregas/aula-02.md`

````markdown
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

```bash
docker compose up -d
cd sistema-emprestimo && ./mvnw spring-boot:run   # em um terminal
cd sistema-margem     && ./mvnw spring-boot:run   # em outro
cd sistema-analise    && ./mvnw spring-boot:run   # em outro
cd sistema-emprestimo/request && ./make_request.sh emprestimo.json
```

## Quem fez o quê

| Parte | Quem |
| --- | --- |
| ADR-002 — domínio | Gabriel Moreira (trouxe o domínio), Rodrigo Maia (redação) |
| `sistema-emprestimo` | ... |
| `sistema-margem` | ... |
| `sistema-analise` | ... |
| `IA.md` / revisão | ... |

## Observação sobre as contas do GitHub

Os commits estão nas contas pessoais de cada integrante; a tabela de
matrículas está no [README](../../README.md#equipe).
````

> **Preencham o "quem fez o quê" de verdade.** É a única parte que só vocês sabem, e é
> literalmente o que a rubrica de comunicação técnica pede — nomear autoria por parte.

### 4.12 · As contas do GitHub, daqui para frente

Os commits já feitos não têm conserto retroativo, e refazer histórico às vésperas da
correção é pior que o problema.

```bash
# em cada maquina, dentro do repositorio, ANTES do proximo commit
git config user.name  "1456295"
git config user.email "o e-mail cadastrado na sua conta do GitHub"

# confere se os commits estao sendo atribuidos
git log --format='%an <%ae>' -5
```

> **E há trabalho por fazer:** o histórico mostra sete integrantes com contribuição bem
> desigual — um deles com um único commit, de README. As receitas acima são doze tarefas
> independentes; distribuí-las entre quem commitou pouco resolve o problema técnico e o de
> participação ao mesmo tempo.

---

*Commit `ccce7b7` · árvore de trabalho limpa · build e testes executados no Codespace da
equipe com Java 21 · `mvn test` em sistema-analise: 8 testes, 1 erro, BUILD FAILURE.*
