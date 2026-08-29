# Distribuicao de atividades — Auditoria Equipe 06

Baseado na auditoria do commit `ccce7b7`. Cada integrante tem um arquivo proprio em `audit-files/` com instrucoes detalhadas.

## Quadro geral

| # | Integrante | Arquivo | Tempo est. | Tarefas |
|---|---|---|---|---|
| 1 | Henrique Miguel de Jesus | [atividades-henrique-miguel.md](atividades-henrique-miguel.md) | ~30 min | Tag da entrega, healthcheck Postgres, eliminar TestPublisher |
| 2 | Paulo Henrique Nunes Vanderley | [atividades-paulo-henrique.md](atividades-paulo-henrique.md) | ~25 min | Cabecalhos `ce_` (32 ocorrencias), devcontainer Java 21 |
| 3 | Rodrigo Pretes Maia | [atividades-rodrigo-pretes.md](atividades-rodrigo-pretes.md) | ~40 min | Folha de rosto `aula-02.md`, seed `data.sql` |
| 4 | Jose Ricardo Ciola Bricio | [atividades-jose-ricardo.md](atividades-jose-ricardo.md) | ~1h | UUID por evento, `emprestimoId`, consumidor tolerante |
| 5 | Diego Bruno Dantas Diogenes | [atividades-diego-bruno.md](atividades-diego-bruno.md) | ~1h30 | IdempotenciaTest margem, remover contextLoads, H2 |
| 6 | Gabriel Moreira da Silva de Faria | [atividades-gabriel-moreira.md](atividades-gabriel-moreira.md) | ~1h20 | Retorno dos `send()`, protecao `ce_id` nulo |
| 7 | Natan de Almeida Figueiredo | [atividades-natan-almeida.md](atividades-natan-almeida.md) | ~1h | Emprestimo/margem bug, `emprestimoId` correto, git config |

## Dependencias entre tarefas

```
Paulo (cabecalhos ce_) -----> pode ser feito primeiro, sem dependencias
Henrique (tag + healthcheck) -----> independente
Rodrigo (folha de rosto) -----> independente
Jose Ricardo (UUID + campos) -----> Natan depende disso (emprestimoId)
Diego (testes) -----> pode rodar depois do Paulo (ce_ ja trocado)
Gabriel (send + null guard) -----> independente
Natan (margem bug + emprestimoId) -----> depende do Jose Ricardo
```

## Ordem sugerida de merge

1. Paulo (cabecalhos) + Henrique (tag/healthcheck) — sem conflitos entre si
2. Jose Ricardo (UUID + campos) — muda estrutura dos eventos
3. Natan (margem bug + emprestimoId) — depende dos campos novos
4. Gabriel (send + null guard) — toca nos mesmos servicos, merge manual possivel
5. Diego (testes) — valida tudo no final
6. Rodrigo (folha de rosto) — sem conflito, pode entrar a qualquer momento

## Lembrete para todos

Antes de qualquer commit:

```bash
git config user.name "SUA_MATRICULA"
git config user.email "seu-email-do-github"
```

## Itens do checklist resolvidos por tarefa

| Checklist | Status atual | Quem resolve |
|---|---|---|
| 03 - Cabecalhos `ce_*` | FALHA | Paulo |
| 05 - Idempotencia 3x=1x | PARCIAL | Diego |
| 10 - Build em maquina limpa | FALHA | Paulo (devcontainer) + Diego (contextLoads) |
| 12 - Sufixo na lista fechada | FALHA | Henrique |
| 17 - Commits na propria conta | FALHA | Todos (daqui para frente) |
| 18 - Tag `entrega-aula-02` | FALHA | Henrique |

## Bugs de codigo resolvidos por tarefa

| Bug | Quem resolve |
|---|---|
| 2.1 - Java nao declarado | Paulo (devcontainer) |
| 2.2 - ce_id nulo = loop infinito | Gabriel |
| 2.3 - ce_id nao e id do evento | Jose Ricardo + Natan |
| 2.4 - Marca processado antes de publicar | Gabriel (retorno send) |
| 2.5 - Emprestimo aumenta margem | Natan |
| 2.6 - Consumidor tolerante nao demonstrado | Jose Ricardo |
| 2.7 - Healthcheck banco errado | Henrique |
| 2.8 - Seed no schema.sql | Rodrigo |
