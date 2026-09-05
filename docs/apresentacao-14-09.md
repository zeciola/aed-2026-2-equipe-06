# Roteiro da Apresentação Técnica — 14/09/2026

**AED · Arquitetura Reativa e Event-Driven · Equipe 06**  
**Tempo Total:** 10 minutos (rigorosamente cronometrado)  
**Apresentadores (3 vozes):** Henrique Miguel de Jesus, Natan de Almeida Figueiredo e Gabriel Moreira da Silva de Faria  

---

## Estrutura dos 10 Minutos

| Momento | Tempo | Quem fala | Conteúdo |
|---|---|---|---|
| **1. O Domínio e o Problema** | 0:00 - 1:30 (1m30s) | **Henrique Miguel** | O que é a CredFolha, por que empréstimo consignado exige consistência eventual e os 3 serviços autônomos. |
| **2. O Modelo de Eventos e Contratos** | 1:30 - 3:30 (2m00s) | **Natan de Almeida** | Envelope CloudEvents 1.0 (`ce_*`), ordenação por partição com chave no CPF e os contratos no Kafka. |
| **3. Demonstração Prática ao Vivo** | 3:30 - 6:30 (3m00s) | **Gabriel Moreira** | Execução do fluxo feliz e do fluxo de exceção com compensação (Saga em ação no terminal/Kafka UI). |
| **4. A Decisão que Mais Custou** | 6:30 - 8:30 (2m00s) | **Henrique Miguel** | Coreografia vs Orquestração (ADR-006), o desafio de não ter ponto central e como tratamos margem presa/DLQ. |
| **5. O que Ficou de Fora e Encerramento** | 8:30 - 10:00 (1m30s) | **Natan de Almeida** | Dívidas técnicas assumidas com maturidade (ADR-003) e próximos passos. Abertura para arguição. |

---

## Roteiro Detalhado por Voz

### Momento 1: O Domínio e o Problema (Henrique Miguel · 1m30s)
* **Objetivo:** Prender a atenção da banca demonstrando domínio do negócio real.
* **Falas-chave:**
  - *"Bom dia a todos e ao professor Sândalo. A Equipe 06 desenvolveu a arquitetura de crédito consignado para a CredFolha."*
  - *"Em consignado, conceder crédito além da margem permitida por lei é crime; mas reter a margem do funcionário sem liberar o empréstimo bloqueia o cliente injustamente."*
  - *"Para resolver isso sem acoplamento temporal nem transações distribuídas, desenhamos 3 microsserviços Maven 100% desacoplados: o `sistema-emprestimo` que capta a proposta, o `sistema-margem` que custodia o limite legal, e o `sistema-analise` que decide a concessão de crédito."*

### Momento 2: Modelo de Eventos e Contratos (Natan de Almeida · 2m00s)
* **Objetivo:** Demonstrar o rigor técnico e a conformidade de contratos.
* **Falas-chave:**
  - *"Nossos serviços não compartilham nenhuma classe Java nem biblioteca comum. A comunicação é 100% assíncrona orientada a fatos de domínio."*
  - *"Usamos o envelope CloudEvents 1.0 em modo binário com cabeçalhos padronizados: `ce_id` com UUID único de cada emissão para idempotência, `ce_time` em ISO-8601 UTC para nosso agregador temporal, e `ce_type` versionado com compatibilidade BACKWARD."*
  - *"A chave de todas as mensagens no Kafka é o CPF do cliente, garantindo que mesmo com 3 partições por tópico, os eventos do mesmo trabalhador sejam processados em ordem estrita."*

### Momento 3: Demonstração ao Vivo (Gabriel Moreira · 3m00s)
* **Objetivo:** Mostrar o sistema funcionando na prática com clareza.
* **Ações no terminal:**
  1. Disparar a requisição de exemplo:
     ```bash
     cd sistema-emprestimo/request && ./make_request.sh cpf_analise_reprovada.json
     ```
  2. Mostrar no log do `sistema-emprestimo`: `HTTP 202 Accepted` e publicação de `emprestimo.solicitado.v1`.
  3. Mostrar no log do `sistema-margem`: consumo idempotente, débito gravado e emissão de `margem.reservada.v1`.
  4. Mostrar no log do `sistema-analise`: reprovação de crédito determinística e publicação de `analise.reprovada.v1`.
  5. Mostrar no log do `sistema-margem`: **A Saga de Compensação em ação!** Consumo da reprovação, estorno no razão contábil e publicação de `margem.liberada.v1`.

### Momento 4: A Decisão Técnica que Mais Custou (Henrique Miguel · 2m00s)
* **Objetivo:** O ponto alto da apresentação que atinge o nível Exemplar na rubrica.
* **Falas-chave:**
  - *"A decisão mais difícil do nosso projeto, registrada no ADR-006, foi escolher entre Coreografia e Orquestração."*
  - *"Optamos pela Coreografia Reativa: nenhum serviço manda nos outros; cada um reage a fatos do domínio. Mas reconhecemos explicitamente o custo disso: em uma coreografia, responder 'em que ponto o pedido está agora?' exige correlação por `solicitacaoId` ou projeções analíticas, porque não há uma tabela de estado central."*
  - *"E nos preparamos para o pior cenário: se a compensação falhar, nosso `DefaultErrorHandler` retenta falhas transitórias com backoff de 1s e envia falhas permanentes para a Dead Letter Queue (`analise.reprovada.v1.dlq`), preservando os cabeçalhos CloudEvents para auditoria e reprocessamento."*

### Momento 5: O que Ficou de Fora e Encerramento (Natan de Almeida · 1m30s)
* **Objetivo:** Mostrar maturidade de engenharia antes mesmo de ser perguntado.
* **Falas-chave:**
  - *"Assumimos no ADR-003 dívidas técnicas deliberadas: mantivemos a execução via Maven local em vez de Dockerfiles individuais para ter ciclos de desenvolvimento rápidos e logs limpos."*
  - *"Não implementamos cálculo de score complexo porque o foco da disciplina é arquitetura e resiliência."*
  - *"Com três serviços, cinco tópicos de negócio, DLQs configuradas, agregador temporal em Event Time e compensação da Saga completa, estamos prontos para as perguntas da banca. Obrigado!"*

---

## Dicas para a Apresentação
1. **Ambiente já de pé antes de começar:** Rode `docker compose up -d` 5 minutos antes e suba os serviços em terminais organizados lado a lado.
2. **Sem tour de código linha a linha:** A banca quer ver arquitetura, fluxo e decisões, não getters e setters.
3. **Segurança no cronômetro:** Se faltar 1 minuto para os 10 minutos, pule direto para o fechamento.
