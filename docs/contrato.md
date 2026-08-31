# Contrato do evento `margem.reservada.v1`

Documento voltado a **quem consome** o evento — outro time, outro serviço.
Descreve o que o `sistema-margem` publica hoje, com a grafia exata do código.

---

## 1. Identificação

| Item | Valor |
|---|---|
| Tipo do evento (`ce_type`) | `margem.reservada.v1` |
| Tópico Kafka | `margem.reservada.v1` |
| Agregado produtor | Margem (`sistema-margem`) |
| Origem (`ce_source`) | `sistema-margem` |
| Envelope | CloudEvents 1.0, modo *binary* (metadados em headers Kafka, domínio no corpo JSON) |
| Consumidores previstos | `sistema-analise` (análise de crédito) e sistema de crédito |
| Frequência estimada | ~500 eventos/dia |
| Partições do tópico | 3 |

Grafia definida em `sistema-margem/src/main/resources/application.yaml`
e no header `ce_type` em `MargemService.gerarMargemReservadaEvent`
(`sistema-margem/src/main/java/br/com/puc/aed/sistemamargem/service/MargemService.java:114`).

---

## 2. Campos

### 2.1 Metadados (headers Kafka — envelope CloudEvents)

| Nome | Tipo | Obrigatório | SIGNIFICADO |
|---|---|---|---|
| `ce_id` | string (UUID v4) | sim | Identificador único **desta publicação** do evento; é a chave que o consumidor usa para deduplicar — ver o mesmo `ce_id` duas vezes significa entrega repetida, não uma segunda reserva. |
| `ce_type` | string | sim | Nome e versão do contrato (`margem.reservada.v1`); muda de sufixo (`.v2`) somente quando o significado de algum campo mudar. |
| `ce_source` | string | sim | Serviço que produziu o evento (`sistema-margem`); serve para rastrear origem, não para roteamento. |
| `ce_time` | string ISO-8601 (UTC) | sim | Instante em que a reserva de margem **foi efetivada no banco do produtor** — é o "ocorridoEm" do fato de negócio, não o horário de leitura pelo consumidor. |
| `ce_specversion` | string | sim | Versão da especificação CloudEvents usada no envelope (`1.0`); diz como ler os headers, nada sobre o negócio. |

### 2.2 Corpo (JSON)

| Nome | Tipo | Obrigatório | SIGNIFICADO |
|---|---|---|---|
| `cpf` | string (11 dígitos, sem pontuação) | sim | CPF do cliente **cuja margem foi reservada**; identifica a pessoa dona da margem consumida, e é também a chave de partição do registro. |
| `solicitacaoId` | string (UUID) | sim | Identificador da **solicitação de empréstimo** que causou esta reserva (é o `emprestimoId` recebido em `emprestimo.solicitado.v1`); é a correlação entre a reserva e o pedido original — use-o, e não o `ce_id`, para juntar este evento ao fluxo do empréstimo. |

**Nota de compatibilidade com o mapa de eventos.** O planejamento previa
`EventoId`, `ocorridoEm`, `cpf`, `codigoVerba` como carga mínima. Na
implementação, `EventoId` e `ocorridoEm` viraram `ce_id` e `ce_time` no envelope
CloudEvents (mesma informação, outro lugar), e `codigoVerba` **não é publicado**:
a verba é detalhe interno do cálculo de margem e nenhum consumidor previsto
decide nada com ela. Se algum consumidor passar a precisar, entra como campo
novo e opcional — não como troca de significado de campo existente.

**O que o evento NÃO diz.** Ele afirma apenas que a margem foi reservada com
sucesso para aquela solicitação. Não afirma que o empréstimo foi aprovado, nem
que o valor foi liberado, nem carrega o valor reservado. Reserva de margem é
condição necessária, não decisão de crédito.

---

## 3. Formato de datas

Todo carimbo de tempo é **ISO-8601 em UTC, com sufixo `Z`** — a serialização de
`java.time.Instant`:

```
2026-08-31T14:23:07.512Z
```

**Nunca epoch** (nem segundos, nem milissegundos). Um número inteiro no lugar de
uma data é ambíguo entre segundos e milissegundos, não carrega fuso e não é
legível em log; o contrato proíbe as duas formas. Datas viajam como string
ISO-8601 e nada mais.

---

## 4. Chave de partição e ordem garantida

**Chave:** o `cpf` do cliente (`MargemService.java:105-109`, o `ProducerRecord`
recebe `cpf` como key).

**O que isso garante:** todos os eventos do mesmo CPF caem na mesma partição,
logo são **entregues em ordem entre si**. Para um mesmo cliente, o consumidor vê
as reservas na ordem em que aconteceram.

**O que isso NÃO garante:** não há ordem global entre CPFs diferentes. O evento
do CPF `A` e o do CPF `B` podem chegar em qualquer ordem, porque estão em
partições diferentes. Nenhum consumidor pode assumir ordenação total no tópico.

Consequência prática para o consumidor: escale o consumo por partição (até 3
consumidores no grupo, pelo número de partições), e não paralelize o
processamento de eventos de um mesmo CPF, sob pena de perder a ordem que a chave
garante.

---

## 5. Regra de compatibilidade: **BACKWARD**

**Escolha:** BACKWARD — um consumidor já atualizado para o esquema novo consegue
ler os eventos escritos com o esquema antigo.

**Por quê:** neste fluxo o produtor é um só (`sistema-margem`) e os consumidores
são vários e independentes (`sistema-analise`, sistema de crédito, e o que vier).
Com BACKWARD, o produtor pode evoluir e os consumidores atualizam quando puderem,
sem quebra e sem coordenação de deploy. É também a regra que casa com o histórico
retido no tópico: eventos antigos continuam legíveis por código novo.

**O que isso permite:** adicionar campo opcional (com default) e remover campo
obrigatório.
**O que isso proíbe:** adicionar campo obrigatório sem default, renomear campo,
estreitar tipo.

**O que nenhuma regra de registry pega — e é o motivo deste documento.**
Mudar o *significado* de um campo mantendo o tipo passa por qualquer validação de
esquema. Se `solicitacaoId` passasse a apontar para o contrato assinado em vez da
solicitação, continuaria sendo `string`, o esquema continuaria válido, o registry
aprovaria — e todo consumidor que correlaciona por ele passaria a correlacionar
outra coisa, silenciosamente, sem um único erro. Mudança de significado é
**sempre** `v2`, nunca uma alteração dentro de `margem.reservada.v1`.

---

## 6. Exemplo de carga (valores fictícios)

**Headers Kafka:**

```
ce_specversion: 1.0
ce_type:        margem.reservada.v1
ce_source:      sistema-margem
ce_id:          3f2b8c14-9a7e-4d51-b0c6-71ee2d5a9f30
ce_time:        2026-08-31T14:23:07.512Z
```

**Chave do registro:** `"11111111111"`

**Corpo:**

```json
{
  "cpf": "11111111111",
  "solicitacaoId": "8c1d47b2-5e39-4a86-9f10-2b7c33d4e5a1"
}
```

Leitura em português: em 31/08/2026 às 14:23:07 UTC, o `sistema-margem`
reservou margem do cliente de CPF `11111111111` para a solicitação de empréstimo
`8c1d47b2-5e39-4a86-9f10-2b7c33d4e5a1`. Se este mesmo `ce_id` chegar de novo,
é reentrega — descarte.
