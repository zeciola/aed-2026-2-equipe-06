# ADR-003 — Ambiente de execução e empacotamento

## Status

Proposta · 2026-08-29 · Equipe 06

## Contexto

O projeto é composto por três serviços Maven independentes — `sistema-emprestimo`,
`sistema-margem` e `sistema-analise` — sobre Java 21 e Spring Boot 4.1, apoiados por Kafka e
Postgres provisionados pelo `compose.yml` na raiz do repositório.

O checklist da Aula 02 exige, no item 10, que o projeto compile e suba **numa máquina
limpa**, seguindo o próprio README. Até esta semana a equipe nunca havia verificado esse
item de fora das máquinas de desenvolvimento, onde o Java 21 já estava instalado desde a
preparação de ambiente da Aula 01.

A verificação foi feita em 29/08/2026, num GitHub Codespace recém-criado a partir do
repositório. O resultado:

- a imagem padrão do Codespace traz **Java 11**. O `./mvnw test` falha no
  `maven-compiler-plugin`, antes de compilar qualquer classe do projeto. O Java 21 existe na
  máquina, instalado via SDKMAN, mas **nada no repositório informa isso** — o README apenas
  lista "Java 21" como pré-requisito, e um pré-requisito escrito em prosa não configura
  ambiente nenhum;
- com o Java 21 colocado manualmente no `PATH`, o projeto compila e os testes de domínio,
  de serviço e o `IdempotenciaTest` do `sistema-analise` passam — este último exercitando
  Kafka embutido em cerca de 16 segundos;
- ainda assim, `mvn test` termina em `BUILD FAILURE`. A causa é a classe `contextLoads`
  gerada pelo Spring Initializr em cada módulo: ela sobe o contexto real da aplicação, que
  tenta conectar em `localhost:15430` e `localhost:19093`, e falha com
  `IllegalStateException: Failed to load ApplicationContext` quando a infraestrutura não
  está de pé;
- em paralelo, notou-se que o `healthcheck` do Postgres no `compose.yml` consulta um banco
  chamado `app`, enquanto o banco efetivamente criado é `aed`. O serviço funciona, mas o
  container permanece marcado como *unhealthy*.

Um Codespace novo é, na prática, a definição operacional de "máquina limpa" — e é também o
caminho de menor esforço para quem for avaliar o projeto sem instalar nada. Hoje, esse
caminho falha no primeiro comando do README.

Há ainda uma restrição de contexto que pesa nesta decisão: a Etapa 2 do projeto (Aula 03 —
contrato do evento e primeiro agregador), com prazo em 23/08, está pendente. Qualquer
decisão de ambiente tomada agora precisa custar pouco tempo de equipe.

## Decisão

O repositório passa a declarar o próprio ambiente de execução, e o empacotamento dos
serviços fica fora de escopo por ora. Concretamente:

1. **O toolchain é declarado no repositório.** Um `.devcontainer/devcontainer.json` fixa a
   imagem `mcr.microsoft.com/devcontainers/java:1-21-bookworm` e habilita a feature
   `docker-in-docker`, de modo que tanto o build quanto o `docker compose up` funcionem
   dentro do Codespace sem intervenção manual.

2. **O `compose.yml` provisiona apenas infraestrutura.** Kafka, Kafka UI e Postgres sobem
   por Compose; os três serviços continuam sendo executados por `./mvnw spring-boot:run`,
   um por terminal. **Não haverá Dockerfile por serviço nesta etapa.**

3. **O build deixa de depender de infraestrutura externa.** As classes `contextLoads`
   herdadas do Initializr são removidas; a cobertura passa a vir dos testes que exercitam
   comportamento real, com Kafka embutido e banco em memória, no modelo já validado pelo
   `IdempotenciaTest` do `sistema-analise`.

4. **O `healthcheck` do Postgres passa a consultar o banco `aed`.**

## Alternativas consideradas

**Containerizar os três serviços — um Dockerfile por módulo, mais três serviços no
`compose.yml`.** Tecnicamente é a alternativa mais barata do que aparenta: o Spring Boot faz
*relaxed binding* de variáveis de ambiente, de modo que `SPRING_KAFKA_BOOTSTRAP_SERVERS` e
`SPRING_DATASOURCE_URL` sobrescrevem os `application.yaml` sem que nenhum arquivo de
configuração precise mudar. Foi recusada por três razões. Primeira, o empacotamento em
imagem não é pedido em nenhum enunciado da disciplina e não corresponde a critério algum das
rubricas publicadas — o esforço não se converte em nada avaliado. Segunda, a demonstração de
idempotência depende de ler o log de cada serviço isoladamente, e `docker compose logs`
intercala os três, tornando a evidência mais difícil de mostrar, não mais fácil. Terceira, o
ciclo de edição fica mais lento: cada alteração de código passa a exigir reconstrução de
imagem. A decisão pode ser revista na Aula 06, quando o projeto estiver estável.

**Documentar o Java 21 melhor no README, em vez de declarar ambiente.** Recusada porque foi
exatamente o que existia até agora. O README já trazia "Pré-requisitos: Java 21" e isso não
impediu o build de falhar num ambiente limpo. Instrução que depende de alguém ler não é
ambiente declarado.

**`spring-boot:build-image`, com Buildpacks e sem Dockerfile.** Descartada junto com a
containerização, pelos mesmos motivos. Fica registrada aqui porque, se em alguma etapa
futura o empacotamento for necessário, este é o caminho de menor esforço para o Spring Boot
e deve ser avaliado antes de escrever um Dockerfile à mão.

**Testcontainers para substituir os `contextLoads`.** É a solução tecnicamente mais elegante:
subiria Postgres e Kafka reais, efêmeros, dentro do próprio teste. Recusada por escopo e
tempo — adiciona dependência, exige Docker disponível no ambiente de build e aumenta a
duração da suíte, numa semana em que a Etapa 2 está atrasada. Kafka embutido mais banco em
memória resolve o problema com o que o projeto já tem.

## Consequências aceitas

**O ambiente declarado cobre um caminho, não todos.** O `.devcontainer` vale para Codespaces
e para VS Code com a extensão Dev Containers. Quem clonar o projeto e compilar direto na
própria máquina continua responsável por ter Java 21 no `JAVA_HOME` — o README passa a dizer
isso explicitamente, em vez de assumir.

**Não existe artefato publicável dos serviços.** Se em alguma etapa futura for preciso
executar o projeto num ambiente que não seja a máquina de um integrante — uma esteira de
CI, um servidor de demonstração, um avaliador que só tenha Docker — esta decisão terá de ser
revista, e o custo será maior do que seria agora, porque haverá mais três serviços e mais
configuração para externalizar. A equipe aceita esse custo diferido em troca do tempo que
ele libera nesta semana.

**Subir o projeto continua sendo um procedimento de quatro comandos em quatro terminais.**
Isso é aceitável para demonstração ao vivo, e é inclusive preferível para evidenciar o
comportamento de cada consumidor separadamente. Não é aceitável como forma de entregar
software, e não pretendemos que seja.

**Remover os `contextLoads` deixa o `sistema-emprestimo` temporariamente sem nenhum teste.**
É dívida reconhecida, e não disfarçada: o teste de idempotência do `sistema-margem`, exigido
pelo enunciado da Aula 02, e um teste do `EmprestimoService` pertencem à mesma frente de
trabalho e vêm logo em seguida. Trocar um teste que falha sempre por nenhum teste só é
progresso se o teste de verdade vier — e é isso que estamos nos comprometendo a fazer.

**Esta decisão foi mantida deliberadamente barata.** A Etapa 2 — contrato do evento e
primeiro agregador, com prazo em 23/08 — tem prioridade sobre qualquer melhoria de ambiente,
e o plano de recuperação dela está registrado em
[`docs/entregas/aula-03.md`](../entregas/aula-03.md). Se o ambiente tivesse consumido a
semana, teria sido a decisão errada, por mais correto que fosse o resultado técnico.
