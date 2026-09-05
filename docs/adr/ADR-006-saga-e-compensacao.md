# ADR-006 — Saga e Compensação: Coreografia vs. Orquestração

## Status

Aceita · 2026-09-05 · Equipe 06

## Contexto

No domínio de crédito consignado da CredFolha (estabelecido no [ADR-002](ADR-002-dominio-do-projeto.md)), a liberação de um empréstimo depende de operações encadeadas entre serviços autônomos:
1. `sistema-emprestimo`: recebe a solicitação HTTP e emite `EmprestimoSolicitadoEvent` (`emprestimo.solicitado.v1`).
2. `sistema-margem`: consome a solicitação, valida margem disponível e debita o valor, emitindo `MargemReservadaEvent` (`margem.reservada.v1`).
3. `sistema-analise`: consome a reserva de margem e avalia o crédito do cliente.

Caso a análise de crédito resulte em aprovação (`AnaliseAprovadaEvent`), o fluxo segue para liquidação. Contudo, quando a análise **reprova** o cliente (`AnaliseReprovadaEvent`), a margem salarial que já havia sido debitada no `sistema-margem` fica indevidamente bloqueada.

Como as transações de banco locais (Postgres) não podem e não devem ser coordenadas via transações distribuídas (2PC / XA) em uma arquitetura orientada a eventos assíncrona, faz-se necessária a implementação do padrão **Saga** com **transação compensatória** para desfazer o débito de margem.

A questão central de desenho arquitetural é: **Como coordenar as etapas da Saga e a compensação entre os três serviços — por Orquestração centralizada ou por Coreografia reativa?**

## Decisão

Adotar **Coreografia Reativa Descentralizada** para a coordenação da Saga e do fluxo de compensação.

Concretamente:
1. **Sem coordenador central:** Não haverá um serviço dedicado de orquestração nem uma máquina de estados centralizada. Cada microsserviço reage autonomamente aos fatos de domínio publicados nos tópicos Kafka.
2. **Reação direta à reprovação:** O `sistema-margem` declara um listener (`MargemCompensacaoListener`) no tópico `analise.reprovada.v1` com grupo consumidor próprio (`sistema-margem-compensacao`).
3. **Ação compensatória atômica:** Ao consumir o fato `AnaliseReprovadaEvent`, o `sistema-margem` executa no mesmo `@Transactional`:
   - Deduplicação atômica via chave primária na tabela `evento_processado` usando o `ce_id`.
   - Inserção de registro de estorno na tabela `margem` (tipo `CREDITO`), anulando o débito anterior na apuração do saldo `SUM(valor)`.
   - Publicação do fato de compensação `MargemLiberadaEvent` no tópico `margem.liberada.v1`.
4. **Confirmação pós-commit:** O offset Kafka só é confirmado (`ack.acknowledge()`) após a persistência bem-sucedida do estorno no banco de dados.

## Alternativas consideradas

### Alternativa 1: Orquestrador de Saga dedicado (`sistema-orquestrador`)
Criar um quarto serviço que manteria a máquina de estados do pedido (`SOLICITADO`, `MARGEM_RESERVADA`, `EM_ANALISE`, `APROVADO`, `COMPENSANDO`, `CANCELADO`) enviando comandos pontuais (`ReservarMargemCommand`, `CancelarMargemCommand`).

*Razão da recusa:*
- Adicionaria um ponto único de complexidade e acoplamento temporal no pipeline.
- Transformaria os eventos de fatos ocorridos no passado em comandos imperativos disfarçados, contrariando o princípio fundamental da disciplina de que eventos são imutáveis e representam fatos consumados.
- Introduziria latência adicional em cada transição de estado.

### Alternativa 2: Compensação via chamada síncrona HTTP/REST
Ao reprovar o crédito, o `sistema-analise` faria um `DELETE` ou `POST /margem/estornar` diretamente contra a API do `sistema-margem`.

*Razão da recusa:*
- Viola frontalmente os requisitos de desacoplamento e entrega *at-least-once*: se o `sistema-margem` estivesse temporariamente fora do ar, o `sistema-analise` falharia ou precisaria gerenciar retentativas síncronas na memória.
- Acoplaria a disponibilidade em tempo real dos dois serviços.

### Alternativa 3: Update/Delete direto no registro original de margem
Em vez de gravar um novo registro de crédito compensatório, executar `UPDATE margem SET status = 'CANCELADO'` ou `DELETE FROM margem`.

*Razão da recusa:*
- Viola o princípio de **Event Sourcing / Ledger Imutável**: para fins de auditoria regulatória de crédito consignado, é mandatório manter o histórico do débito e o histórico posterior do estorno.
- O modelo contábil do `sistema-margem` opera por razão de lançamentos (`SUM(valor)`), onde estornos são representados por novos lançamentos com sinal oposto.

## Consequências aceitas

1. **Rastreabilidade descentralizada da Saga ("Em que ponto o fluxo parou?"):**
   - Em uma arquitetura coreografada, não há uma tabela única que diga o estado global de uma solicitação em tempo real.
   - *Mitigação:* A correlação ponta a ponta é mantida via campo de domínio `solicitacaoId` em todos os eventos e rastreabilidade pelo envelope CloudEvents (`ce_id`). É possível reconstruir o estado completo consultando os tópicos no Kafka UI ou executando projeções analíticas.

2. **O que acontece se a compensação falhar e a margem ficar presa?**
   - Se o `sistema-margem` encontrar um erro transitório (ex: Postgres momentaneamente fora do ar ao tentar gravar o estorno), o mecanismo de retentativas limitadas do `DefaultErrorHandler` com backoff reprocessa a mensagem.
   - Se a falha for permanente (ex: falha irrecuperável de integridade), a mensagem é enviada ao tópico de Dead Letter Queue (`analise.reprovada.v1.dlq`) preservando todos os cabeçalhos `ce_*` e registrando a causa da falha.
   - *Mitigação operacional:* A equipe de sustentação monitora a DLQ e dispara reprocessamento idempotente assim que a inconsistência for sanada. Em última instância, o relatório de margem agregada e reconciliação diária de batimento contábil detecta margens sem solicitação correspondente ativa.

3. **Complexidade de tópicos adicionais:**
   - O surgimento de `margem.liberada.v1` e das DLQs aumenta a quantidade de tópicos no cluster Kafka de 5 para 8 tópicos. A equipe aceita essa sobrecarga operacional em favor da visibilidade granular do ciclo de vida da transação.
