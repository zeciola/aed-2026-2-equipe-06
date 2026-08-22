# AGENTS.md — padrões obrigatórios deste repositório

Este arquivo orienta qualquer pessoa (ou agente de IA) que for implementar
código neste repositório. Os padrões abaixo vêm do enunciado da Aula 02
(`doc/AED - Aula 02 - Atividade Pratica e Desafio.pdf`) e do demo de
referência do professor (`demo-kafka-idempotencia/`) — são verificados na
correção. Desvio sem justificativa em ADR custa nota.

## Domínio do projeto

Empréstimo consignado: reserva de margem salarial e análise de crédito em
sistemas externos, com compensação (cancelamento da reserva de margem) quando
a análise reprova. Ver [docs/adr/ADR-002-dominio-do-projeto.md](docs/adr/ADR-002-dominio-do-projeto.md)
para o fluxo completo, os quatro critérios atendidos e as consequências
aceitas. Não redefina o domínio sem atualizar o ADR primeiro.

## Serviços

- `sistema-emprestimo` — publisher. Recebe a solicitação de empréstimo via
  HTTP (`POST /emprestimos/solicitar`), persiste o registro local do
  empréstimo solicitado (Postgres, tabela `emprestimo`) e publica
  `EmprestimoSolicitadoEvent` no tópico `emprestimo.solicitado.v1`.
- `sistema-margem` — consumidor idempotente. Escuta `emprestimo.solicitado.v1`,
  calcula a margem consignável disponível do CPF (soma da tabela `margem`) e
  publica `MargemReservadaEvent` (aprovado, `margem.reservada.v1`) ou
  `MargemRecusadaEvent` (recusado, `margem.recusada.v1`). Dedup via tabela
  `evento_processado`, chave `evento_id`.
- `sistema-analise` — consumidor idempotente. Escuta `margem.reservada.v1`, decide
  aprovação/reprovação de crédito (regra determinística por CPF, documentada como
  "caixa-preta" no ADR-002 — não modela score real) e publica `AnaliseAprovadaEvent`
  (`analise.aprovada.v1`) ou `AnaliseReprovadaEvent` (`analise.reprovada.v1`). Dedup via
  tabela própria `evento_processado_analise` (nome distinto da de `sistema-margem`, mesmo
  schema Postgres).

Os três são projetos Maven **independentes**: sem POM pai, sem módulo de
contrato compartilhado. Cada lado declara a própria classe do evento
(`EmprestimoSolicitadoEvent` existe duplicada nos dois `domain`, de propósito
— é o ponto do desacoplamento por JSON, não por JAR compilado).

Postgres é o banco de runtime dos dois serviços (`compose.yml`, porta 15430,
banco `aed`), schema criado via `schema.sql` com `spring.sql.init.mode=always`.
Persistência é `JdbcTemplate` com SQL explícito — sem Spring Data JDBC, sem
ORM. Ver seção "Pacotes e nomenclatura" para onde cada peça mora.

## Commits semânticos (obrigatório)

Todo commit segue `tipo(escopo): descrição curta no presente`, escopo
recomendado quando o commit afeta só um serviço (`sistema-emprestimo` ou
`sistema-margem` — é o padrão já usado no histórico do repositório).

| Tipo | Quando usar |
|---|---|
| `feat` | novo código de funcionalidade (evento, controller, service, listener...) |
| `fix` | correção de comportamento incorreto |
| `docs` | ADR, README, IA.md, folha de rosto, comentários de documentação |
| `chore` | configuração de repositório: `.gitignore`, `compose.yml`, scaffold do Initializr |
| `test` | testes automatizados |
| `refactor` | mudança de estrutura sem alterar comportamento |

Exemplos (do próprio histórico): `docs: registra ADR-002 do domínio (empréstimo
consignado)`, `feat(sistema-margem): adiciona eventos de margem recusada e
reservada`.

A ordem de commit importa: o ADR-002 entra ANTES de qualquer `.java` (ver
checklist do enunciado — `git log --reverse` é conferido). Nosso histórico já
respeita isso.

## Pacotes e nomenclatura (obrigatório, verificado na correção)

| Regra | Certo | Errado |
|---|---|---|
| Raiz do nome em português, sufixo em inglês | `EmprestimoSolicitadoEvent`, `SolicitarEmprestimoVO` | `LoanRequestedEvent`, `EmprestimoSolicitadoEvento` |
| Quatro pacotes, com estes nomes | (raiz), `controller`, `domain`, `service` | `events`, `entities`, `vos`, `utils`, `impl` |
| Sufixos da lista fechada | `Application`, `Config`, `Controller`, `Listener`, `Service`, `Repository`, `Event`, `VO` | `Util`, `Helper`, `Manager`, `Impl`, `DTO`, `Producer` |
| O nome descreve o papel, não o fornecedor | `MargemService`, `kafkaTemplate` como campo, nunca como sufixo de classe | `MargemKafkaPublisher` |
| `domain` não conhece framework de infraestrutura | `domain` só importa biblioteca padrão e anotações de serialização (Jackson) | `Emprestimo` importando `org.springframework.data` ou `org.apache.kafka` |
| Repositório é porta no `domain`, adapter no `service` | `domain.MargemRepository` (interface) implementada por `service.MargemRepository` (classe `@Repository` com `JdbcTemplate`) | `CrudRepository`/`Persistable` vazando para dentro do `domain` |
| `@Transactional` só no `Service` | transação começa e termina no serviço de aplicação | `@Transactional` no `Listener` ou no `Controller` |

Pacote raiz: `br.com.puc.aed.sistemaemprestimo` e `br.com.puc.aed.sistemamargem`
— minúsculo, sem hífen, mesmo nome do diretório do módulo.

## O evento

- Nome no particípio, descrevendo um fato ocorrido, nunca um comando
  (`EmprestimoSolicitado`, `MargemReservada`, não `SolicitarEmprestimo`).
- Classe imutável explícita: campos `private final`, sem setter, cópia
  defensiva de coleção. Não usar `record` — os mecanismos precisam ficar
  à vista.
- Identidade própria: um `eventoId` que vive no cabeçalho `ce_id`, distinto
  do id da entidade de negócio (`emprestimo.id`, `margem.id`). A chave de
  deduplicação é o `eventoId`, nunca o id da entidade.
- Datas em ISO-8601 (`Instant`/`toString()`), nunca epoch.
- Consumidor tolerante: declare menos campos do que o publisher publica.
  Campos desconhecidos são ignorados (`@JsonIgnoreProperties(ignoreUnknown = true)`
  no lado consumidor quando a classe local for um subconjunto da publicada).

## O publisher (`sistema-emprestimo`)

- Envelope CloudEvents 1.0 em modo binário, com underscore:
  `ce_specversion`, `ce_id`, `ce_source`, `ce_type`, `ce_time` nos cabeçalhos
  — é a grafia do enunciado e do demo do professor (`servico-pedidos`/`servico-estoque`),
  não o hífen (`ce-specversion`).
- `type` numa grafia só, versionada: `dominio.entidade.fato.v1`
  (`emprestimo.solicitado.v1`, `margem.reservada.v1`, `margem.recusada.v1`).
- Publicar com chave de partição — o CPF, menor unidade cuja ordem o negócio
  exige (mesmo CPF sempre na mesma partição).
- O retorno do `send()` tem dono: tratado no próprio `Service`, nunca ignorado.
- A API HTTP responde 202, não 200, quando o efeito ainda não aconteceu
  (`ResponseEntity.accepted()`).

## O consumidor idempotente (`sistema-margem`)

- Recebe o mesmo evento mais de uma vez e produz efeito UMA vez.
- Memória do que já foi processado: tabela `evento_processado` com
  `evento_id` como chave primária.
- Registro da dedup é atômico: `INSERT INTO evento_processado (evento_id)
  VALUES (?) ON CONFLICT DO NOTHING`, verificando linhas afetadas (1 = novo,
  0 = reentrega) — nunca `findById().ifPresentOrElse(...)` (não é atômico,
  abre janela de corrida entre duas entregas concorrentes do mesmo evento).
- O efeito de negócio (gravar `margem`, decidir aprovar/recusar) e o registro
  da deduplicação acontecem no MESMO método `@Transactional`, ou seja, no
  mesmo commit.
- A confirmação do offset (`ack.acknowledge()`) vem DEPOIS do commit da
  transação — é o que caracteriza at-least-once (`ack-mode: manual_immediate`,
  `enable-auto-commit: false`).
- Declarar menos campos do que o publisher publica, de propósito — campos
  desconhecidos devem ser ignorados (consumidor tolerante).
- Teste automatizado que entregue o mesmo evento três vezes e verifique o
  efeito único (pendente — ver checklist abaixo).

## Estrutura de diretórios obrigatória

```
aed-2026-2-equipe-06/
├── README.md
├── compose.yml
├── docs/
│   ├── adr/ADR-002-dominio-do-projeto.md
│   ├── entregas/aula-02.md
│   └── IA.md
├── sistema-emprestimo/
├── sistema-margem/
└── sistema-analise/
```

Caminho fora do padrão não é encontrado na correção. Não criar pastas
adicionais fora deste layout sem justificar em ADR.

## Ordem de criação (não altera nota, mas é conferida)

1. `README.md`
2. `docs/adr/ADR-002-dominio-do-projeto.md` — ANTES da primeira linha de código
3. `docs/IA.md` — ao longo da semana
4. `docs/entregas/aula-02.md` — por último

## Checklist rápido antes de commitar

- Nome de evento não tem raiz CRUD (`create`, `update`, `delete`, `salvar`).
- Nenhuma data em epoch no corpo da mensagem.
- Os cinco cabeçalhos `ce_*` obrigatórios estão presentes e usam underscore
  (`ce_specversion`, `ce_id`, `ce_source`, `ce_type`, `ce_time`) — hoje o
  código publica com hífen (`ce-id` etc.), precisa alinhar.
- O `eventoId` publicado no `ce_id` é um id próprio do evento, não o id da
  entidade de negócio reaproveitado — hoje `sistema-emprestimo` publica
  `emprestimo.getId()` como `ce-id`, precisa de um `eventoId` separado.
- `domain/` não importa `org.apache.kafka` nem `org.springframework` de
  infraestrutura (JDBC, Kafka) — só anotações Jackson.
- `@Transactional` não aparece em `controller/` nem em `Listener`.
- Nenhum dado pessoal real (CPF, e-mail, telefone) em código ou cargas de
  exemplo.
