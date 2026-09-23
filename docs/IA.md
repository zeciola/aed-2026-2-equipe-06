# Registro de uso de IA

## Aula 02

_A preencher ao longo da semana com pelo menos três interações relevantes. Para cada uma:
o que foi pedido, o que a ferramenta sugeriu, o que foi aceito e — obrigatoriamente — pelo
menos uma coisa que foi RECUSADA, com a razão técnica da recusa._

### 2026-08-22 — Desenho do `sistema-analise` (terceiro serviço, análise de crédito)

**Pedido:** criar o terceiro serviço do domínio (`sistema-analise`, o segundo "sistema
externo" exigido pelo ADR-002), a partir do diagrama de event storming e do fluxo que ainda faltava depois da reserva de margem.

**Sugestão da ferramenta:** três abordagens de arquitetura — (1) módulo Maven independente,
consumidor Kafka idempotente, espelhando a estrutura já usada em `sistema-margem`; (2)
embutir a lógica de análise dentro do próprio `sistema-margem`; (3) `sistema-margem` chamar
`sistema-analise` via REST síncrono em vez de publicar evento. Também apontou um problema de
modelagem antes de qualquer código: a tabela `evento_processado` de `sistema-margem` e a
tabela de dedup do novo serviço ficariam no mesmo banco/schema Postgres (`aed`/`public`), e o
`ce_id` que `sistema-margem` publica em `margem.reservada.v1` reaproveita o mesmo id recebido
em `emprestimo.solicitado.v1` — se as duas tabelas tivessem o mesmo nome, o dedup do novo
serviço nunca processaria nada, porque o id já estaria lá.

**Aceito:**
- Abordagem (1) — módulo Maven independente `sistema-analise`, mesma estrutura de pacotes
  (`controller`/`domain`/`service`) e mesmo padrão de persistência (`JdbcTemplate` + interface
  no `domain`/adapter no `service`) já validado em `sistema-emprestimo`/`sistema-margem`.
- Tabela de dedup com nome próprio (`evento_processado_analise`), para não colidir com a de
  `sistema-margem` no mesmo schema Postgres.
- `sistema-analise` consome `margem.reservada.v1` diretamente (grupo `sistema-analise`,
  distinto de `sistema-margem` para não competir pela mesma partição), sem criar um tópico
  "análise solicitada" novo — o evento já existente carrega o que é necessário (CPF e id da
  solicitação).
- Regra de decisão determinística por CPF (último dígito par aprova, ímpar reprova) em vez de
  sorteio aleatório — reproduzível em teste e demonstração, sem precisar mudar o evento que já
  existe para carregar valor do empréstimo.
- No código novo do `sistema-analise`, gerar um `eventoId` (`UUID.randomUUID()`) próprio a
  cada publicação, em vez de repassar o id recebido — mesmo sem ainda termos corrigido esse
  mesmo problema em `sistema-emprestimo`/`sistema-margem`.

**Recusado:**
- Abordagem (2), embutir a análise em `sistema-margem` — recusada porque colapsaria dois dos
  quatro critérios exigidos pelo ADR-002 num único serviço (a análise deixaria de ser um
  "sistema externo" separado) e misturaria duas responsabilidades de domínio distintas
  (custódia de margem × decisão de crédito) na mesma base de código e no mesmo deploy.
- Abordagem (3), chamada REST síncrona entre os serviços — recusada porque contraria o padrão
  de integração exigido pelo enunciado (Kafka, entrega at-least-once, consumidor idempotente)
  e acoplaria a disponibilidade dos dois serviços em tempo real, o que a integração por evento
  existe justamente para evitar.
- Corrigir agora os cabeçalhos `ce-*` para o padrão underscore (`ce_*`) do enunciado/demo do
  professor nos três serviços — recusado nesta rodada porque expande o escopo para além do
  serviço novo; fica registrado como dívida técnica (já também anotada no `AGENTS.md`).
- Implementar já o caminho de compensação (análise reprovada → cancelar reserva de margem),
  a liberação do empréstimo e a notificação ao cliente — recusado nesta rodada por escopo; o
  próprio ADR-002 já havia registrado a saga de compensação como trabalho da aula 05.

## Unidade IV — Projeto final (Resiliência, DLQ, Saga e Documento de Arquitetura)

### 2026-09-06 — Implementação da compensação de margem (Saga coreografada)

**Pedido:** implementar o fluxo de compensação quando a análise de crédito reprova
— desfazer a reserva de margem que já havia sido aplicada.

**Sugestão da ferramenta:** três abordagens: (1) criar um `MargemCompensacaoListener`
com grupo consumidor próprio (`sistema-margem-compensacao`) no tópico
`analise.reprovada.v1`, registrando crédito compensatório com evento
`MargemLiberadaEvent` no particípio; (2) resolver com `UPDATE margem SET
status = 'CANCELADO'` no registro original de débito; (3) introduzir um
orquestrador central que coordenasse a compensação via comandos imperativos.

**Aceito:**
- Abordagem (1) — listener dedicado com grupo próprio, estorno via INSERT de
  crédito (tipo `CREDITO`) na tabela `margem`, anulando o débito por soma
  algébrica (`SUM(valor)`), e publicação de `margem.liberada.v1` como fato de
  compensação.
- Deduplicação atômica do evento de reprovação na mesma tabela `evento_processado`
  do `sistema-margem`, dentro da mesma transação `@Transactional`.
- Campo `motivo` no `MargemLiberadaEvent`, propagado do `AnaliseReprovadaEvent`,
  para auditoria.

**Recusado:**
- Abordagem (2), `UPDATE`/`DELETE` no registro original — recusada porque viola
  o princípio de ledger imutável: o modelo contábil de margem opera por razão
  de lançamentos e apagar o débito original eliminaria a trilha de auditoria.
  Conforme discutido na aula 05 (Event Sourcing), corrigir o passado apagando o
  passado destrói a sequência de fatos. A ferramenta insistiu nessa abordagem
  como "mais simples", mas ela não atende o requisito regulatório de auditabilidade.
- Abordagem (3), orquestrador central — recusada pelos mesmos motivos registrados
  no ADR-006: ponto único de falha, acoplamento temporal, e transformação de
  fatos em comandos disfarçados.

### 2026-09-07 — Retentativa, DLQ e tratamento de falhas

**Pedido:** implementar política de retentativa com limite e Dead Letter Queue para
falhas permanentes e transitórias.

**Sugestão da ferramenta:** (1) usar `DefaultErrorHandler` com
`ExponentialBackOffWithMaxRetries` e `DeadLetterPublishingRecoverer`; (2) usar
retentativa infinita com `SeekToCurrentErrorHandler` (deprecated); (3) criar
tópicos de retry separados (retry-5s, retry-1min, retry-10min) para não segurar
a partição.

**Aceito:**
- Abordagem (1) — `DefaultErrorHandler` do Spring Kafka 3.x com backoff
  exponencial (1s→30s teto, 8 retentativas), e `DeadLetterPublishingRecoverer`
  publicando no tópico `<original>.dlq` com bytes originais preservados e
  cabeçalhos CloudEvents intactos.
- `ErrorHandlingDeserializer` delegando ao `JacksonJsonDeserializer`, para que
  JSON malformado vá direto para a DLQ sem retentativa e sem travar a partição.
- Template dedicado com `ByteArraySerializer` para a DLQ, preservando o payload
  original sem reserialização.

**Recusado:**
- Abordagem (2), retentativa infinita — recusada porque é exatamente o
  antipadrão descrito na aula 01: o evento nunca avança, a partição inteira
  para atrás dele, e o lag cresce sem exceção visível. A ferramenta sugeriu
  "retries = Integer.MAX_VALUE com backoff" como "configuração resiliente",
  mas isso transforma falha permanente em bloqueio silencioso de fila.
- Abordagem (3), tópicos de retry separados — anotada como desafio opcional no
  enunciado; ficou de fora do escopo desta entrega por complexidade operacional
  adicional sem ganho proporcional para o cenário demonstrado.

### 2026-09-20 — Documento de arquitetura e Makefile

**Pedido:** consolidar o documento de arquitetura (docs/arquitetura.md) com as 8
seções do enunciado, e criar Makefile para facilitar demonstração e avaliação.

**Sugestão da ferramenta:** (1) gerar o documento de arquitetura referenciando
"conforme visto na aula 06" e "conforme o enunciado pede"; (2) escrever para
quem vai manter o sistema sem contexto da disciplina.

**Aceito:**
- Abordagem (2) — documento escrito para quem vai manter o sistema em janeiro,
  sem referências à disciplina, aulas ou enunciado. Linguagem de documento
  técnico profissional.
- Makefile com targets para provisionar infraestrutura, rodar cenários de teste
  e gravar demos com asciinema.

**Recusado:**
- Abordagem (1), referências à disciplina — recusada porque o item 16 do
  checklist proíbe explicitamente e porque um documento de arquitetura que só
  faz sentido para quem acompanhou a disciplina não é um documento de
  arquitetura. A ferramenta sugeriu "AED · Equipe 06" no cabeçalho como
  "contextualização", mas isso é exatamente o que o professor definiu como
  insuficiente.