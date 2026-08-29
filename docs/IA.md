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