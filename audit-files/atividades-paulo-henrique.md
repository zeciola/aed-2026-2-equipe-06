# Atividades — Paulo Henrique Nunes Vanderley (@paulnune)

**Matricula:** 1456295
**Tempo estimado:** ~25 min

---

## Tarefa 1: Trocar `ce-` por `ce_` em todos os cabecalhos (ref: auditoria 4.3)

**Problema:** Os cabecalhos CloudEvents estao grafados com hifen (`ce-id`, `ce-type` etc.), mas o enunciado exige underscore (`ce_id`, `ce_type`). Sao 32 ocorrencias nos tres servicos.

**O que fazer:**

```bash
grep -rl '"ce-' --include=*.java . \
  | xargs sed -i 's/"ce-\(specversion\|id\|source\|type\|time\)"/"ce_\1"/g'
```

**Conferir:**

```bash
# nenhum hifen pode sobrar
grep -rho '"ce[-_][a-z]*"' --include=*.java . | sort | uniq -c
# esperado: 8 "ce_id"  6 "ce_source"  6 "ce_specversion"  6 "ce_time"  6 "ce_type"
```

> Isso afeta arquivos em `sistema-emprestimo`, `sistema-margem` e `sistema-analise`. Confirme que TODAS as ocorrencias foram trocadas.

---

## Tarefa 2: Criar `.devcontainer/devcontainer.json` com Java 21 (ref: auditoria 4.2)

**Problema:** O Codespace padrao traz Java 11. Sem configuracao, `./mvnw test` morre no compilador. O repositorio nao declara a versao de Java em lugar nenhum alem do README.

**O que fazer:**

Criar o diretorio e o arquivo:

```bash
mkdir -p .devcontainer
```

Criar `.devcontainer/devcontainer.json`:

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

Atualizar a secao "Como rodar" do `README.md`, adicionando logo apos a linha de pre-requisitos:

```markdown
No GitHub Codespaces o ambiente ja vem pronto (ver `.devcontainer/`).
Em Codespace antigo, sem rebuild: `sdk use java 21.0.10-ms`.
```

**Conferir:** O arquivo existe e o JSON e valido.

---

## Antes de commitar

```bash
git config user.name "1456295"
git config user.email "seu-email-do-github"
```
