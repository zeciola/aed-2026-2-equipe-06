# Aula 03 — Folha de rosto

## O que foi feito nesta etapa

- **Parte A** — Contrato formal do evento `margem.reservada.v1`, documentado em [docs/contrato.md](../contrato.md):
  - Identificação completa com envelope CloudEvents 1.0 em modo binário (`ce_*`).
  - Chave de partição por CPF garantindo ordenação total por cliente.
  - Regra de compatibilidade **BACKWARD** justificada pela dinâmica entre múltiplos consumidores independentes e evolução sem quebra.
- **Parte B** — Segundo consumidor do tópico `margem.reservada.v1` com grupo próprio (`sistema-margem-agregador`), agregando reservas por janela de tempo:
  - `MargemAgregadorListener.java` (controller) escutando `margem.reservada.v1` em grupo independente.
  - `MargemAgregadorService.java` (service) calculando métricas temporais agregadas.
  - `MargemAgregadaVO.java` (domain) encapsulando o resultado agregado observável.

---

## As quatro perguntas da Etapa 2

### 1. Qual é a pergunta de negócio agregada?
> *"Quantas reservas de margem salarial foram comprometidas por janela temporal e como essa demanda se distribui entre os funcionários?"*

**Justificativa de negócio:** Empresas de crédito consignado precisam monitorar a taxa de comprometimento da folha em intervalos regulares para auditoria de risco e controle de liquidez operacional. Essa métrica responde à previsão de faturamento e volumetria de empréstimos sem onerar a base transacional de escrita.

### 2. Qual é a janela escolhida e por quê?
> **Tumbling Window (janela fixa e desarticulada) de 1 hora (3600 segundos).**

**Justificativa:** A apuração de margem e limites operacionais de consignado funciona em ciclos contábeis e horários de corte pré-determinados. Uma *tumbling window* garante partições temporais disjuntas — cada reserva pertence a exatamente uma janela horária, evitando contagem duplicada e simplificando a reconciliação com os lotes das instituições financeiras.

### 3. Qual relógio foi escolhido e por quê?
> **Relógio do Evento (*Event Time*), extraído do cabeçalho `ce_time` (ISO-8601 UTC).**

**Justificativa técnica:** No processamento de eventos, usar o relógio do processamento (*processing time*) seria fatal para a integridade histórica do sistema. Se uma falha de infraestrutura exigir o reprocessamento retroativo do tópico Kafka a partir do offset zero, o *processing time* colapsaria todo o histórico de reservas na janela do momento da execução, distorcendo o relatório. Com *event time*, o reprocessamento reconstrói exatamente o estado real em que as reservas ocorreram no passado.

### 4. O que acontece com retardatários (*late events*)?
> **Eventos atrasados são admitidos e agregados na janela original de ocorrência (retroativa), atualizando o total histórico sem descartar dados.**

**Comportamento:** Caso um evento chegue atrasado (por exemplo, devido a retenção transitória de partição ou latência de rede), o `MargemAgregadorService` computa o início da janela baseado no `ce_time` original do fato e atualiza o acumulador correspondente daquela fatia de tempo. O resultado agregado observável é atualizado idempotentemente, garantindo consistência eventual completa.

---

## Por onde começar a leitura

1. [docs/contrato.md](../contrato.md) — O contrato do evento `margem.reservada.v1`.
2. `sistema-margem/.../controller/MargemAgregadorListener.java` — Segundo consumidor com grupo `sistema-margem-agregador`.
3. `sistema-margem/.../service/MargemAgregadorService.java` — Agregação em janela tumbling orientada a *event time*.
4. `sistema-margem/.../domain/MargemAgregadaVO.java` — Value Object imutável do resultado agregado.

---

## Quem fez o que

| Parte | Quem |
|---|---|
| Contrato `margem.reservada.v1` | Gabriel Moreira (revisão), José Ricardo (redação técnica) |
| Agregador por Janela de Tempo | Henrique Miguel de Jesus (Listener, Service e VO) |
| Folha de rosto `aula-03.md` | Henrique Miguel de Jesus (redação técnica das 4 perguntas) |
