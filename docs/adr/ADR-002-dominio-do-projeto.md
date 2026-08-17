# ADR-002 — Domínio do projeto

## Status
Aceita · 2026-08-17 · Equipe 06

## Contexto
Empresas de crédito consignado precisam decidir, de forma auditável, se aprovam um pedido
de empréstimo respeitando o limite de margem salarial consignável do funcionário. Esse
processo depende da integração com pelo menos dois sistemas externos — um que controla a
margem disponível do funcionário e outro que faz a análise de crédito — e precisa desfazer
reservas já feitas quando qualquer uma dessas etapas falha.

O domínio foi trazido por Gabriel Moreira da Silva de Faria, a partir de experiência real em
empresa de crédito consignado (aqui referida pelo nome fictício "CredFolha", já que o
repositório é público).

## Decisão
Um funcionário solicita um empréstimo consignado. O serviço verifica a documentação enviada
— recusando o pedido internamente se ela estiver incompleta ou inválida, sem sequer consultar
sistemas externos. Se a documentação é válida, o serviço reserva a margem consignável do
funcionário no sistema externo de margem; margem reservada aciona a análise de crédito no
sistema externo de análise; análise aprovada libera o empréstimo e notifica o cliente.

Como a decisão atende cada um dos quatro critérios:
 - ponto de decisão com regra de negócio: a verificação de documentação, que recusa o pedido
   internamente (documentação incompleta ou inválida) antes de consultar qualquer sistema
   externo
 - sistema externo: o sistema de margem (reserva ou recusa a margem consignável) e o sistema
   de análise de crédito (aprova ou reprova o empréstimo)
 - caminho de exceção com compensação: quando a análise de crédito reprova, a reserva de
   margem já feita precisa ser cancelada (estornada) no sistema de margem
 - algo que valha reprocessar: um relatório de margem comprometida por funcionário,
   reconstruído a partir do histórico de eventos de reserva e cancelamento de margem — útil
   para auditoria e para dar suporte a decisões futuras de crédito

## Alternativas consideradas
A equipe não considerou formalmente outro domínio: o processo de crédito consignado partiu
direto da experiência real de um dos integrantes e já atendia claramente aos quatro
critérios exigidos. A alternativa implícita seria o domínio padrão sugerido no enunciado
(pedido → estoque, já coberto pela demonstração), descartada por não ter um caminho de
exceção com compensação real nem um motivo genuíno para reprocessar — ela travaria na aula
05 exatamente como o enunciado descreve.

## Consequências aceitas
Ficam fora do escopo: cálculo de juros e parcelas, renegociação de contrato existente, e
múltiplos empréstimos simultâneos por funcionário.

A decisão final de crédito continua sendo uma "caixa-preta" do sistema externo de análise —
não internalizamos score nem critérios de aprovação de crédito. Isso deixa nosso único ponto
de decisão interno concentrado na verificação de documentação, o que é mais limitado do que
ter a decisão de crédito propriamente dita dentro do domínio; aceitamos essa limitação porque
reflete a realidade do processo.

Na aula 05, a Saga de compensação vai precisar tratar o cancelamento de margem como reação a
uma decisão de negócio externa (reprovação da análise), não a uma falha técnica de
comunicação — o que provavelmente exige um estado intermediário explícito (algo como
"aguardando análise") que o demo-kafka-idempotencia não modela. O relatório de margem
comprometida, por sua vez, exige manter um read model de projeção separado do agregado de
escrita, o que é complexidade de infraestrutura adicional em relação a uma consulta direta.s