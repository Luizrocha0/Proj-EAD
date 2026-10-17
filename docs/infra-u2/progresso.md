# Registro de Progresso — Rota Vital — Infraestrutura U2

Documento de acompanhamento da execução por etapas do projeto **Rota Vital** para a entrega da Unidade 2 (Infraestrutura de Software: Containers, TDD e Deploy Automatizado).

---

## Histórico de Etapas

| Etapa | Descrição | Estado | SHA / Data |
|---|---|---|---|
| **Etapa 0** | Diagnóstico da Base e Linha de Base de Testes | **Concluída** | `b410dfcf8a6caa39c1fde36fbaa3e52c39863bef` (16/10/2026) |
| **Etapa 1** | Story Map e Critérios de Aceite da Release 1 | **Concluída** | 17/10/2026 |
| **Etapa 2** | E1: Base de Containers (Dockerfile, Compose, PostgreSQL) | **Concluída** | 17/10/2026 |
| **Etapa 3** | E2: H1 — Cadastro e Recuperação Persistente (TDD) | **Concluída** | 17/10/2026 |
| **Etapa 4** | E2: H2 — Listagem e Consulta Persistente (TDD) | Pendente | — |
| **Etapa 5** | E2: H3 — Remoção Persistente (TDD) | Pendente | — |
| **Etapa 6** | E2: Integração e Gates na Esteira | Pendente | — |
| **Etapa 7** | E3: Branches, GitHub Flow e Deploy Automatizado | Pendente | — |
| **Etapa 8** | Documentação e Apresentação | Pendente | — |
| **Etapa 9** | Auditoria Final | Pendente | — |

---

## Etapa 0: Diagnóstico da Base e Linha de Base

- **Estado:** Concluída
- **Data de Execução:** 16/10/2026
- **Commit HEAD Inspecionado:** `b410dfcf8a6caa39c1fde36fbaa3e52c39863bef`
- **Mensagem do Commit:** `fix(so): alinhe medições e relatório aos dados brutos da U1`
- **Branch:** `main` (sincronizada com `origin/main`)
- **Regra de Integridade:** Nenhum arquivo de código de produção ou teste pré-existente foi modificado ou excluído.

---

### 1. Inventário Detalhado dos Componentes do Repositório

#### 1.1. Estruturas de Dados Manuais da U1 (C e Java)
- **Estruturas em C (`aed-c/`):**
  - `aed-c/include/lista.h`, `bolsa.h`, `fila.h`, `requisicao.h`: Interfaces e definições de tipos para lista simplesmente encadeada de bolsas e fila FIFO de requisições hospitalares.
  - `aed-c/src/u1/lista.c`: Implementação de lista encadeada com nós dinâmicos (`malloc`/`free`), inserção na cabeça ($O(1)$), busca linear ($O(n)$) e remoção ($O(n)$).
  - `aed-c/src/u1/fila.c`: Implementação de fila FIFO com ponteiros de início (`cabeca`) e fim (`cauda`).
  - `aed-c/tests/test_lista.c`: 23 asserções de teste unitário para lista encadeada.
  - `aed-c/tests/test_fila.c`: 31 asserções de teste unitário para fila FIFO.
  - `aed-c/Makefile`: Script de compilação com sanitizers (`-fsanitize=address,undefined`).
- **Estruturas Equivalentes em Java (`src/main/java/com/rotavital/aed/u1/`):**
  - `ListaEstoque.java`: Lista encadeada pura baseada em nós manuais `NoBolsa`, sem uso de `List` ou coleções da JVM. A inserção ocorre na cabeça (registros mais recentes primeiro). A consulta e a remoção operam na ocorrência mais recente em caso de IDs duplicados.
  - `FilaRequisicoes.java`: Fila FIFO encadeada pura baseada em nós `NoRequisicao`.
  - `docs/equivalencia-u1.md`: Documentação pedagógica da equivalência direta C $\leftrightarrow$ Java.

#### 1.2. Aplicação Spring Boot e Endpoints REST
- **Domínio (`src/main/java/com/rotavital/domain/`):**
  - `Bolsa.java`: Entidade imutável com campos `id` (String), `tipoSanguineo` (String), `volumeMl` (double), `dataColeta` (LocalDate), `validade` (LocalDate), `doador` (String).
  - `Requisicao.java`: Entidade de solicitação hospitalar com `id`, `hospitalId`, `tipoSanguineo`, `quantidadeBolsas`, `prioridade` (enum `NORMAL`, `URGENTE`, `EMERGENCIA`), `dataHora`.
- **Camada de Serviço e Controle (`src/main/java/com/rotavital/`):**
  - `EstoqueController.java`: Endpoints REST sob `/api/estoque`:
    - `POST /api/estoque`: Adiciona bolsa. Recebe `@RequestBody Map<String, String> body`. Retorna 200 com JSON `{"mensagem": "Bolsa adicionada ao estoque", "tamanho": N}`.
    - `GET /api/estoque/{id}`: Consulta bolsa por ID. Retorna 200 com JSON completo ou 404.
    - `DELETE /api/estoque/{id}`: Remove bolsa pelo ID. Retorna 200 com JSON `{"mensagem": "Bolsa removida", "tamanho": N}` ou 404.
    - `GET /api/estoque`: Retorna JSON com envelope `{"bolsas": [...], "tamanho": N}` mantendo a ordem de inserção da lista (cabeça para cauda).
  - `EstoqueService.java`: Acoplado à instância estática em memória `private final ListaEstoque estoque = new ListaEstoque();`.
  - `RequisicaoController.java`: Endpoints REST sob `/api/requisicoes`:
    - `POST /api/requisicoes`: Enfileira requisição com corpo `@RequestBody Map<String, String> body` (`id`, `hospitalId`, `tipoSanguineo`, `quantidadeBolsas`, `prioridade`). Retorna 200 com `{"mensagem": "Requisição enfileirada", "tamanho": N}`.
    - `POST /api/requisicoes/atender`: Desenfileira e atende a próxima requisição (FIFO). Retorna 200 com `{"mensagem": "Requisição atendida", "requisicao": {...}, "restantes": N}` ou `{"mensagem": "Fila vazia — nenhuma requisição pendente"}`.
    - `GET /api/requisicoes/proxima`: Consulta a próxima requisição sem desenfileirar (peek). Retorna 200 com dados da requisição ou `{"mensagem": "Fila vazia"}`.
    - `GET /api/requisicoes`: Retorna JSON com envelope `{"requisicoes": [...], "tamanho": N}` preservando a ordem FIFO.
  - `RequisicaoService.java`: Acoplado à fila em memória `private final FilaRequisicoes fila = new FilaRequisicoes();`.
  - `DuplicateController.java`: Endpoint REST sob `/api/duplicates`:
    - `GET /api/duplicates/detect`: Parâmetros query `size` (1..20000), `threads` (1..CPU*4) e `mode` (`platform` ou `virtual`). Retorna métricas de tempo de geração, processamento e duplicatas encontradas.
  - `DataGeneratorService.java`: Gerador sintético determinístico com base em 10 medicamentos pré-definidos (`Random(42)`).
  - `DuplicateDetectionService.java` e `FuzzyMatcher.java`: Algoritmo multithread da U1 (SO) para detecção de duplicatas com thread pool customizado (`ExecutorService` fixo ou virtual threads) e cálculo de similaridade por distância de Levenshtein.
- **Contratos e Resiliência de Tipagem das Requisições REST:**
  Tanto `EstoqueController` quanto `RequisicaoController` utilizam `@RequestBody Map<String, String> body`. Nas fixtures oficiais da U1 (`fixtures/bolsas_u1.json` e `fixtures/requisicoes_u1.json`), os atributos `volumeMl` (ex: `450.0`) e `quantidadeBolsas` (ex: `2`) são literais numéricos JSON. Em clientes REST estritos enviando números nativos sem aspas, o Jackson requer configuração de coerção ou DTOs formais (ex: `BolsaDTO`, `RequisicaoDTO`) para evitar erros `MismatchedInputException`. Esse refinamento é ponto chave de atenção para a Etapa 1.
- **Configurações:**
  - `pom.xml`: Spring Boot 3.3.3, Java 21, dependências `spring-boot-starter-web`, `spring-boot-starter-actuator`, `spring-boot-starter-test`.
  - `src/main/resources/application.properties`: Configurações de porta (`8080`), actuator (`health,info`) e metadados.

#### 1.3. Benchmarks e Medições (`bench/`)
- `bench/measure.ps1`: Script PowerShell para benchmark com carga concorrente contra `http://localhost:8080/api/duplicates/detect`. **Aviso de integridade:** Este script regrava `measurements.csv` e `measurements_agg.csv` com novas coletas ativas; não deve ser executado durante verificações de auditoria ou linha de base para preservar a série canônica de 40 medições fixadas na U1.
- `bench/measurements.csv` e `bench/measurements_agg.csv`: 40 medições empíricas brutas e agregação estatística.
- `bench/plot_speedup.py`: Script para cálculo de agregação e geração do gráfico de speedup. A execução com `--check` valida as 8 configurações sem alterar arquivos. Sem a flag `--check`, regrava `measurements_agg.csv` alterando quebras de linha para CRLF no Windows.
- `bench/build_report.py`: Script de geração de relatório técnico em PDF. Requer a biblioteca externa `reportlab` definida em `bench/requirements.txt`.
- `bench/requirements.txt`: Dependências Python (`matplotlib==3.11.2`, `reportlab==4.4.9`).
- `bench/speedup.png`: Gráfico consolidado de speedup gerado a partir das medições.
- `bench/test_plot_speedup.py`: 7 testes unitários em Python para validação dos cálculos estatísticos.

#### 1.4. Fixtures e Dados Canônicos de Validação da U1 (`fixtures/`)
- `fixtures/bolsas_u1.json`: 5 bolsas de sangue canônicas (`BOLSA-001` a `BOLSA-005`) com dados completos para validação funcional.
- `fixtures/requisicoes_u1.json`: 5 requisições hospitalares canônicas (`REQ-001` a `REQ-005`) com prioridades e hospitais pré-definidos.
- `fixtures/resultados_esperados_u1.json`: Matriz formal dos resultados esperados para atestar paridade e equivalência de estruturas entre as implementações C e Java (tamanhos, ordem da cabeça para cauda, FIFO, buscas e remoções).

#### 1.5. Documentação Técnica e Relatórios da U1 (`docs/` e raiz)
- `docs/analise.md`: Análise técnica do speedup de SO U1 (concorrência na Mesa DJ vs paralelismo na detecção de duplicatas, desbalanceamento de fatias disjuntas, complexidade quadrática de Levenshtein $O(n^2 \cdot L^2)$ e overhead de pools de threads).
- `docs/justificativa.md`: Justificativa teórica da operação de detecção fuzzy de duplicatas, divisão de trabalho em partições e agregação livre de concorrência com `Future.get()`.
- `docs/medicoes.md`: Documentação das 40 medições empíricas da U1 e justificativa das extrapolações teóricas para 100 mil e 1 milhão de registros.
- `docs/speedup.png`: Gráfico consolidado oficial de speedup de SO U1.
- `docs/RotaVital_Threads_Relatorio.pdf`: Relatório formal em PDF de 5 páginas da atividade de SO U1.
- `docs/equivalencia-u1.md`: Guia de equivalência e contratos pedagógicos C $\leftrightarrow$ Java da entrega de AED U1.
- `PLANO_ENTREGA_AED_SO.md`: Documento de planejamento conjunto inicial de AED e SO definindo a visão de unificação do backend.
- `README.md`: Documentação geral da base legada com instruções e visão geral do projeto.

#### 1.6. Infraestrutura Atual (Legado)
- `Dockerfile`: Imagem multi-stage (`eclipse-temurin:21-jdk-alpine` $\rightarrow$ `eclipse-temurin:21-jre-alpine`). Fragilidades: executa `./mvnw package -DskipTests -q` durante o build e não propaga `JAVA_OPTS` no entrypoint `java -jar app.jar`.
- `.github/workflows/ci.yml`: Pipeline com jobs `check-benchmark`, `test-c`, `test-java`, `build-java`, `build-docker`, `health-check` (apenas rodando o JAR localmente) e `deploy-render` (dispara webhook sem checar integridade de branch ou commit SHA).
- `render.yaml`: Blueprint web service com `autoDeploy: false`, sem declaração de banco PostgreSQL.
- Ausências no repositório: Não existem `compose.yaml`, `.env.example`, nem arquivos de migração Flyway.

---

### 2. Resultados da Linha de Base de Testes

Todas as suítes de testes foram executadas no ambiente local para estabelecer a linha de base auditável.

#### 2.1. Testes Java (Spring Boot / JUnit 5)
- **Comando executado:**
  ```powershell
  $env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-25.0.4.101-hotspot"
  .\mvnw.cmd test
  ```
- **Ambiente:** OpenJDK 25.0.4.1 LTS / Apache Maven 3.9.9 Wrapper.
- **Resultado:**
  - `com.rotavital.aed.u1.FilaRequisicaoTest`: 12 testes aprovados (5 classes aninhadas).
  - `com.rotavital.aed.u1.ListaEstoqueTest`: 14 testes aprovados (4 classes aninhadas).
  - `com.rotavital.controller.DuplicateControllerTest`: 5 testes aprovados.
  - `com.rotavital.RotaVitalApplicationTest`: 1 teste aprovado (`contextLoads`).
  - `com.rotavital.service.DuplicateDetectionServiceTest`: 11 testes aprovados.
  - `com.rotavital.util.FuzzyMatcherTest`: 5 testes aprovados.
  - **Total:** 48 testes executados, **0 falhas**, **0 erros**, **0 pulados**. Tempo: entre 10.055s e 14.982s (11.765s auditado na rodada atual; `BUILD SUCCESS`).
- **Observação técnica sobre ambiente:** A execução de `.\mvnw.cmd` sem a variável `JAVA_HOME` explicitamente definida na sessão do PowerShell aborta imediatamente com o erro `Error: JAVA_HOME not found in your environment.`. A definição de `$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-25.0.4.101-hotspot"` é pré-requisito indispensável no ambiente Windows.
- **Diagnóstico Crítico de Cobertura da U1:** Os 48 testes existentes cobrem apenas as estruturas em memória (`ListaEstoqueTest` e `FilaRequisicaoTest`), o detector de duplicatas multithread da U1/SO (`DuplicateControllerTest`, `DuplicateDetectionServiceTest`, `FuzzyMatcherTest`) e o carregamento do contexto (`RotaVitalApplicationTest`). **Não existem testes automatizados legados para `EstoqueController` e `EstoqueService`**. Essa constatação é fundamental para o planejamento das histórias H1, H2 e H3 na Etapa 1.

#### 2.2. Testes em C (Estruturas AED)
- **Tentativa com Makefile nativo (`mingw32-make test`):**
  - **Comando:** `mingw32-make -C aed-c test`
  - **Resultado auditado:** Falha imediata na compilação Windows com a seguinte saída:
    ```text
    mingw32-make: Entering directory 'C:/Users/Mignac/Desktop/Proj-ead/aed-c'
    Já existe uma subpasta ou um arquivo tests.
    Erro ao processar: tests.
    Makefile:41: recipe for target 'tests/test_lista' failed
    mingw32-make: *** [tests/test_lista] Error 1
    ```
  - **Causa raiz:** 
    1. A linha `@mkdir -p tests` no Makefile invoca o interpretador `cmd.exe` no Windows. O comando interno `mkdir` tenta criar duas pastas (`-p` e `tests`). Como a pasta `tests` já existe, o `cmd.exe` emite erro e aborta com código de saída 1, deixando para trás um diretório vazio residual `aed-c/-p/` (removido na higienização da auditoria).
    2. Além disso, as flags padrão `CFLAGS = -fsanitize=address,undefined` exigem `libasan`, ausente na distribuição MinGW 6.3.0 de 32/64 bits para Windows.
  - **Diagnóstico:** No CI (GitHub Actions em `ubuntu-latest`), o comando `make test` executa com sucesso pleno utilizando GCC moderno Linux com suporte nativo a sanitizers.
- **Execução direta local (GCC MinGW sem sanitizers):**
  - **Comandos:**
    ```powershell
    gcc -Wall -Wextra -std=c11 -g -Iaed-c/include aed-c/src/u1/lista.c aed-c/tests/test_lista.c -o aed-c/tests/test_lista.exe; .\aed-c\tests\test_lista.exe; Remove-Item aed-c\tests\test_lista.exe
    gcc -Wall -Wextra -std=c11 -g -Iaed-c/include aed-c/src/u1/fila.c aed-c/tests/test_fila.c -o aed-c/tests/test_fila.exe; .\aed-c\tests\test_fila.exe; Remove-Item aed-c\tests\test_fila.exe
    ```
  - **Resultados:**
    - `test_lista.exe`: **23/23 testes passaram** (Lista vazia: 4, Inserção: 4, Consulta: 4, Remoção: 11).
    - `test_fila.exe`: **31/31 testes passaram** (Fila vazia: 4, Enfileiramento: 4, Desenfileiramento: 13, Reuso: 3, Espiar: 5).
  - **Total em C:** 54 asserções aprovadas com 100% de sucesso. Os binários temporários foram limpos após a execução.

#### 2.3. Testes dos Benchmarks em Python
- **Comando 1 (Unittest):**
  ```powershell
  python -m unittest discover -s bench -p "test_*.py"
  ```
  - **Resultado:** **7/7 testes passaram** em 0.001s (`OK`).
- **Comando 2 (Validação de integridade estatística):**
  ```powershell
  python bench/plot_speedup.py --check
  ```
  - **Resultado:** Sucesso (`Agregado correto: 8 configurações.`).
- **Diagnóstico de scripts auxiliares de benchmark:**
  - `bench/measure.ps1`: Não deve ser executado em auditorias ou verificações de rotina. Requer um servidor ativo na porta 8080 e sobrescreve `measurements.csv` e `measurements_agg.csv` com novos dados de execução, o que destruiria a série histórica de 40 medições fixadas na U1.
  - `bench/build_report.py`: Script de relatório PDF falha com `ModuleNotFoundError: No module named 'reportlab'` no ambiente local caso a biblioteca externa não tenha sido instalada via `pip install -r bench/requirements.txt`. O script é utilitário complementar e não afeta a suíte de validação de testes.
  - `bench/plot_speedup.py` sem `--check`: Deve ser evitado durante verificações rotineiras porque regrava `bench/measurements_agg.csv` alterando terminações de linha para CRLF, sujando o `git status`. Apenas a flag `--check` deve ser utilizada na esteira e em auditorias locais.

---

### 3. Matriz de Lacunas Frente aos Requisitos da U2

| Requisito U2 | Estado Atual no HEAD | Lacuna Identificada | Ação Necessária | Etapa Alvo |
|---|---|---|---|:---:|
| **E1: Orquestração Compose** | Ausente | Não existe `compose.yaml`. Aplicação não possui orquestração de contêineres para subir API e PostgreSQL conjuntamente. | Criar `compose.yaml` com serviços `api` e `db`, volumes nomeados, healthchecks e `depends_on: condition: service_healthy`. | Etapa 2 |
| **E1: Dockerfile Otimizado** | Parcial | O Dockerfile compila usando `-DskipTests` e o runtime não repassa a variável `JAVA_OPTS`. Não há segregação de testes unitários. | Remover `-DskipTests`, garantir execução de testes no build sem exigir Docker-in-Docker, repassar `JAVA_OPTS` no `ENTRYPOINT`. | Etapa 2 |
| **E1: Configuração e Segredos** | Ausente | Não há arquivo `.env.example`. | Criar `.env.example` com valores dummy e assegurar que `.env` real seja ignorado no `.gitignore` e `.dockerignore`. | Etapa 2 |
| **E1: Healthcheck e Conectividade JDBC** | Parcial | Actuator expõe apenas `/actuator/health` genérico de JVM em memória; não avalia saúde do PostgreSQL. | No perfil `infra`, configurar health indicator para validar conectividade com PostgreSQL, reportando `DOWN` em caso de falha de banco. | Etapa 2 |
| **E1 / E2: Segregação Surefire vs Failsafe** | Ausente | `pom.xml` não segrega testes unitários de integração; não possui `maven-failsafe-plugin` configurado. | Configurar `maven-surefire-plugin` para testes unitários (`*Test.java`) e `maven-failsafe-plugin` para testes de integração (`*IT.java`). | Etapa 2 / 6 |
| **E2: Persistência PostgreSQL** | Ausente | `pom.xml` não possui `spring-boot-starter-jdbc`, `postgresql` nem `flyway-core`. Não há scripts SQL em `src/main/resources/db/migration/`. | Adicionar dependências JDBC/Postgres/Flyway e criar migração `V1__criar_tabela_bolsas.sql`. | Etapa 2 / 3 |
| **E2: Arquitetura de Perfis (u1 vs infra)** | Ausente | `EstoqueService` acoplado diretamente à `ListaEstoque`. Sem suporte a perfis Spring. | Extrair interface de armazenamento (ex: `EstoqueStorage`); implementar adaptador de memória para perfil `u1` (default) e adaptador JDBC para perfil `infra`. | Etapa 2 / 3 |
| **E2: Testes de Caracterização da U1** | Ausente | Não existem testes automatizados legados para `EstoqueController` e `EstoqueService`. | Escrever testes de caracterização para blindar os contratos e respostas JSON da U1 sob o perfil `u1` antes de refatorar para persistência. | Etapa 1 / 3 |
| **E2: Resiliência de Tipos da API REST (DTOs)** | Parcial | Controllers utilizam `Map<String, String>`, suscetível a erros de deserialização Jackson com números literais das fixtures. | Modelar DTOs tipados ou deserializadores tolerantes a números literais (`volumeMl`, `quantidadeBolsas`) preservando o envelope de resposta. | Etapa 1 / 3 |
| **E2: Suporte a IDs Duplicados e Ordem** | Em memória | `ListaEstoque` aceita IDs repetidos e recupera a ocorrência mais recente (ordem LIFO na cabeça). | O esquema SQL precisa de chave primária técnica autoincrementável (`id_seq` / `serial`) para manter IDs repetidos e recuperar o mais recente via `ORDER BY id_seq DESC`. | Etapa 3 |
| **E2: Ciclo TDD (H1, H2, H3)** | Ausente | Não existem testes automatizados de integração com PostgreSQL real nem testes de aceitação da API persistida. | Implementar histórias H1, H2 e H3 seguindo rigorosamente o ciclo Red $\rightarrow$ Green $\rightarrow$ Refactor com commits rastreáveis. | Etapas 3, 4, 5 |
| **E2: Testes com Testcontainers** | Ausente | Dependências de Testcontainers não configuradas. Sem perfil Maven para testes de integração. | Configurar Testcontainers com PostgreSQL real no `pom.xml` e estruturar testes de integração sem mocks de banco. | Etapa 2 / 6 |
| **E3: Pipeline CI/CD** | Parcial | O job `health-check` inicia o JAR em memória sem banco de dados. O estágio de build não roda testes com Postgres. | Atualizar `.github/workflows/ci.yml` para executar suíte completa de testes, incluindo Testcontainers/PostgreSQL e smoke test containerizado. | Etapa 6 / 7 |
| **E3: Deploy Automatizado (Render)** | Parcial | Disparo de webhook cego (`RENDER_DEPLOY_HOOK_URL`) sem validação do commit SHA validado e sem provimento de banco no Render. | Ajustar trigger para passar ref do commit validado e documentar provisionamento de banco PostgreSQL gerenciado no Render. | Etapa 7 |
| **E3: Governança e GitHub Flow** | Ausente | `main` sem proteção de branches remota verificada; ausência de histórico de PR com gates obrigatórios. | Definir fluxo por PR e documentar exigência dos checks obrigatórios para merge na `main`. | Etapa 7 |

---

### 4. Plano de Ação para a Etapa 1 (Story Map e Critérios)

A próxima etapa (**Etapa 1**) estabelecerá as bases funcionais e de aceitação da Release 1:

1. **Backbone e Story Map da Release 1:**
   - Mapear a jornada do operador do banco de sangue: cadastrar bolsa $\rightarrow$ listar estoque $\rightarrow$ remover bolsa.
   - Definir as três histórias de usuário do escopo da persistência:
     - **H1:** Cadastrar e recuperar bolsa persistida (com comprovação pós-reinício da aplicação/contexto).
     - **H2:** Consultar o estoque persistido (preservando formato do envelope, lista vazia, ordem mais recente e duplicatas de ID).
     - **H3:** Remover bolsa de forma persistente (excluindo estritamente a ocorrência mais recente e mantendo integridade pós-reinício).
2. **Matriz História $\rightarrow$ Tarefa $\rightarrow$ Teste $\rightarrow$ Evidência:**
   - Detalhar para cada história as tarefas específicas de desenvolvimento.
   - Definir os testes em três níveis: Unitário (regras isoladas), Integração (PostgreSQL via Testcontainers) e Aceitação (chamadas HTTP via `TestRestTemplate` / `MockMvc`).
   - Definir as falhas esperadas no ciclo Red para cada tarefa antes da implementação.
3. **Desenho Arquitetural da Persistência e Contratos da U1:**
   - Especificar a interface comum de armazenamento (`EstoqueStorage`).
   - Especificar os adaptadores concretos e perfis Spring: `u1` (ativa `ListaEstoqueStorage` com nós próprios em memória, perfil padrão) e `infra` (ativa `JdbcEstoqueStorage`).
   - Especificar a resiliência de tipos na API REST (DTOs formais para deserialização robusta de JSONs numéricos de fixtures).
   - Especificar o contrato do banco de dados (tabela `bolsas` com campos idênticos ao domínio `Bolsa` e chave substituta serial para resolução temporal $LIFO$).
   - Especificar testes de caracterização dedicados ao perfil `u1` para garantir não regressão.
4. **Critérios de Saída da Etapa 1:**
   - Matriz completa documentada em `docs/infra-u2/story-map.md`.
   - Contratos legados da U1 preservados e vinculados à estratégia de perfis Spring.

---

### 5. Resumo Padronizado de Fechamento da Etapa 0 (Protocolo Obrigatório)

- **Etapa e estado:** Etapa 0 (Diagnóstico da Base e Linha de Base de Testes) — **Concluída**.
- **Mudanças:** 
  - Criação de `docs/infra-u2/progresso.md` contendo inventário exaustivo de código, fixtures, documentação U1 e infraestrutura, evidências de linha de base, diagnóstico de cobertura e mapeamento de lacunas da U2.
  - Nenhum arquivo de código-fonte de produção (`src/main/`, `aed-c/src/`) ou teste existente (`src/test/`, `aed-c/tests/`, `bench/test_*.py`) foi modificado ou excluído, preservando 100% da integridade da U1.
- **Verificação:** 
  - Java 21 / JUnit 5: 48/48 testes aprovados (`mvnw.cmd test`, tempo entre 10.055s e 14.982s, auditado em 10.907s nesta rodada, 0 falhas).
  - C / AED: 54/54 asserções aprovadas via compilação GCC local (23 em `test_lista` e 31 em `test_fila`).
  - Benchmarks Python: 7/7 testes aprovados (`python -m unittest discover -s bench`) e consistência estatística de 8 configurações confirmada (`python bench/plot_speedup.py --check`).
- **Pendências:** 
  - Nenhuma pendência impeditiva para avançar para a Etapa 1.
  - Identificada a ausência total de testes legados para `EstoqueController` e `EstoqueService`, demandando testes de caracterização/aceitação antes e durante o TDD das histórias H1, H2 e H3.
  - Identificadas 15 lacunas estruturais para entrega da U2 (Compose ausente, Dockerfile com `-DskipTests` e sem consumo de `JAVA_OPTS`, ausência de banco de dados/Flyway, ausência de Testcontainers, falta de segregação Surefire/Failsafe, healthcheck sem banco e tipagem estrita de API REST).
- **Próxima etapa:** **Etapa 1 — Story Map e Critérios de Aceite da Release 1** (elaboração de histórias H1, H2, H3, matriz história $\rightarrow$ tarefa $\rightarrow$ teste $\rightarrow$ evidência e arquitetura de perfis `u1` e `infra`).

---

## Etapa 1: Story Map e Critérios de Aceite da Release 1

- **Estado:** Concluída
- **Data de Execução:** 17/10/2026
- **Artefato Produzido:** `docs/infra-u2/story-map.md`
- **Regra de Integridade:** Nenhum arquivo de código de produção (`src/main/`, `aed-c/src/`) ou teste pré-existente foi modificado ou excluído. Nenhuma implementação antecipada foi realizada.

---

### 1. Síntese do Story Map e Backbone da Release 1

- **Backbone da Aplicação:**
  1. *Recepção & Triagem:* Entrada e registro inicial de bolsas de sangue coletadas.
  2. *Gestão de Estoque:* Consulta de inventário, monitoramento de validade e ordenação temporal.
  3. *Dispensação & Baixa:* Remoção de bolsas para atendimento transfusional ou descarte.
  4. *Auditoria & Atendimento:* Validação de consistência, rastreabilidade e integridade pós-reinício.
- **Histórias de Usuário Centrais (Persistência no PostgreSQL):**
  - **H1 — Cadastrar e recuperar bolsa persistida:** Cadastro pela API sob perfil `infra`, gravação no PostgreSQL real e recuperação de todos os atributos após reinicialização da aplicação/contexto.
  - **H2 — Consultar estoque persistido:** Listagem geral em ordem LIFO (mais recente primeiro via `ORDER BY id_seq DESC`), envelope JSON `{"bolsas": [...], "tamanho": N}`, suporte a estoque vazio (`{"bolsas": [], "tamanho": 0}`) e recuperação da ocorrência mais recente em caso de IDs duplicados.
  - **H3 — Remover bolsa de forma persistente:** Exclusão via `DELETE /api/estoque/{id}` atingindo estritamente a ocorrência mais recente ($LIFO$) em duplicatas, preservando ocorrências anteriores intactas, retornando status 404 para identificador inexistente e comprovando persistência da remoção pós-reinício.

---

### 2. Decisões Arquiteturais e Preservação dos Contratos da U1

1. **Desacoplamento por Portas e Adaptadores (Arquitetura Hexagonal Mínima):**
   - Criação da interface `EstoqueStorage` (`salvar`, `buscarPorId`, `removerPorId`, `listarTodas`, `tamanho`).
   - `EstoqueService` deixa de instanciar `new ListaEstoque()` diretamente e passa a receber `EstoqueStorage` via injeção por construtor, delegando 100% de suas operações sem conhecimento do meio físico.
   - Mapeamento explícito de assinaturas:
     - `ListaEstoqueStorage` (perfis `{"u1", "default"}`) delega diretamente para `ListaEstoque`: `salvar -> inserir`, `buscarPorId -> consultar`, `removerPorId -> remover`, `listarTodas -> toArray`, `tamanho -> getTamanho`.
     - `JdbcEstoqueStorage` (perfil `"infra"`) implementa as operações via `JdbcTemplate` com queries SQL e `RowMapper<Bolsa>`.
2. **Estratégia Estrita de Perfis Spring (`u1` vs `infra`):**
   - **Perfil `u1` (Padrão):** Ativa `ListaEstoqueStorage` (anotado com `@Component` e `@Profile({"u1", "default"})`), garantindo ativação mesmo em fallbacks sem perfil explícito. Delega diretamente para a estrutura encadeada em memória `ListaEstoque` com nós próprios `NoBolsa`. Aplicação inicia sem requerer banco de dados, DataSource ou migrações Flyway. Configurado com `spring.autoconfigure.exclude` para `DataSourceAutoConfiguration`, `DataSourceTransactionManagerAutoConfiguration`, `JdbcTemplateAutoConfiguration` e `FlywayAutoConfiguration`, garantindo que o acréscimo futuro de dependências no `pom.xml` não quebre a execução sem banco. Preserva 100% da integridade pedagógica da U1.
   - **Perfil `infra`:** Ativa `JdbcEstoqueStorage` (anotado com `@Component` e `@Profile("infra")`), que opera contra o PostgreSQL 16 via Spring JDBC (`JdbcTemplate`) e migrações Flyway. Ativado no `compose.yaml` e no ambiente Render.
   - **Proibição de Fallback Silencioso:** Sob o perfil `infra`, caso o banco falhe ou esteja indisponível, a aplicação lança exceção imediata (`DataAccessException`), falha a requisição HTTP (HTTP 500/503) e o healthcheck do Actuator reporta `status: DOWN`. Em hipótese alguma há desvio silencioso para a memória.
3. **Gestão Transacional e Blindagem de Concorrência:**
   - **Isolamento de `@Transactional`:** A anotação `@Transactional` é demarcada estritamente dentro de `JdbcEstoqueStorage` (escrita com `@Transactional`, leitura com `@Transactional(readOnly = true)`). Foi vedado o uso de `@Transactional` em `EstoqueService`, pois sob o perfil `u1` (onde `DataSourceTransactionManagerAutoConfiguration` é excluído), a ausência do bean `PlatformTransactionManager` causaria falha fatal de inicialização (`NoSuchBeanDefinitionException: No qualifying bean of type 'TransactionManager'`).
   - **Controle de Concorrência na Remoção:** A deleção LIFO da ocorrência mais recente é atômica via subquery SQL no PostgreSQL (`DELETE FROM bolsas WHERE id_seq = (SELECT id_seq FROM bolsas WHERE id = ? ORDER BY id_seq DESC LIMIT 1)`) ou via CTE avançada com `FOR UPDATE SKIP LOCKED` para alta vazão, prevenindo leituras sujas, contenção de locks e eliminando 404s espúrios entre operações concorrentes.
4. **Resolução de IDs Repetidos e Ordenação LIFO no Banco de Dados:**
   - A tabela `bolsas` utiliza chave substituta autoincrementável `id_seq BIGSERIAL PRIMARY KEY`.
   - O campo de negócio `id` **não possui restrição UNIQUE**, permitindo bolsas com mesmo identificador.
   - A sequência atômica de `id_seq` provê monotonicidade estrita mesmo em inserções concorrentes no mesmo milissegundo, evitando a ambiguidade de ordenação inerente a `criado_em TIMESTAMP`.
   - Índice composto `idx_bolsas_id_seq ON bolsas(id, id_seq DESC)` para busca pontual e subquery de deleção em tempo $O(1)$ sem ordenação em memória.
   - Ordenação LIFO garantida por `ORDER BY id_seq DESC`, matematicamente isomórfica ao percurso `cabeca -> proximo` de `ListaEstoque`.
5. **Resiliência de Tipagem na Deserialização REST:**
   - Modelagem de DTO resiliente (`BolsaRequestDTO`) no `EstoqueController` para tratar números de ponto flutuante literais (`volumeMl: 450.0`) presentes nas fixtures oficiais (`fixtures/bolsas_u1.json`), prevenindo falhas de coerção e `MismatchedInputException` do Jackson.
6. **Estratégia de Testes de Caracterização da U1:**
   - Criação planejada de `EstoqueServiceCaracterizacaoTest` e `EstoqueControllerCaracterizacaoTest` executando sob o perfil `@ActiveProfiles("u1")` antes da implementação de código novo, blindando os contratos históricos contra regressões.

---

### 3. Matriz História $\rightarrow$ Tarefa $\rightarrow$ Teste $\rightarrow$ Evidência

A matriz rastreável completa foi formalizada em `docs/infra-u2/story-map.md`, cobrindo 13 tarefas de desenvolvimento (T1.1–T1.4, T2.1–T2.5, T3.1–T3.4) distribuídas entre H1, H2 e H3, associadas a 27 cenários de testes automatizados balanceadamente distribuídos nos três níveis:
- **Testes Unitários (Nível 1 - 7 cenários):** Validação de delegação em `EstoqueService` (Mocking de `EstoqueStorage` para salvar, consultar, remover, listarTodas e tamanho), teste do adaptador `ListaEstoqueStorageTest` e conversão resiliente de tipos no `BolsaRequestDTO` sem depender de banco ou contexto completo.
- **Testes de Integração (Nível 2 - 12 cenários):** Testes com PostgreSQL real gerenciado via Testcontainers, migrações Flyway, transações JDBC reais, ordenação LIFO, remoção de duplicatas, validação de retorno booleano `false` ao tentar remover ID inexistente no banco (`JdbcEstoqueStorageIT`) e comprovação de sobrevivência/consistência pós-reinício de contexto Spring para H1, H2 e H3 (`BolsaPersistidaIT`).
- **Testes de Aceitação da API REST (Nível 3 - 8 cenários):** Testes ponta a ponta via MockMvc/TestRestTemplate validando contratos HTTP, envelopes JSON (`bolsas` e `tamanho`), status 200/404, tratamento de duplicatas na API e comportamento sob o perfil `infra`.
- **Condições de Falha Red (TDD):** Mapeamento prévio dos erros técnicos esperados no ciclo Red (exceções de deserialização, beans ausentes, falhas de delegação e asserções de contêiner) para cada tarefa antes da implementação correspondente.

---

### 4. Plano de Ação para a Etapa 2 (Base de Containers)

Na próxima etapa (**Etapa 2**), será construída a fundação de infraestrutura de contêineres:
1. **Dockerfile Otimizado:** Remoção de `-DskipTests` no estágio de build, segregação de testes unitários sem Docker-in-Docker, repasse obrigatório de `JAVA_OPTS` no runtime e usuário não privilegiado.
2. **Orquestração Compose (`compose.yaml`):** Serviços `api` (perfil `infra`) e `db` (PostgreSQL 16-alpine), volumes nomeados persistentes, rede dedicada e healthchecks encadeados com `condition: service_healthy`.
3. **Configuração Externa:** Criação de `.env.example` com valores dummy e garantia de isolamento de credenciais no `.gitignore` e `.dockerignore`.
4. **Healthcheck e Actuator:** Configuração de verificação de prontidão do banco PostgreSQL no perfil `infra`.
5. **Segregação Surefire vs Failsafe no `pom.xml`:** Preparação dos plugins Maven para execução de testes unitários (`*Test.java`) no build do Dockerfile e testes de integração (`*IT.java`) na esteira com banco.

---

### 5. Resumo Padronizado de Fechamento da Etapa 1 (Protocolo Obrigatório)

- **Etapa e estado:** Etapa 1 (Story Map e Critérios de Aceite da Release 1) — **Concluída**.
- **Mudanças:** 
  - Criação do documento canônico `docs/infra-u2/story-map.md` contendo: mapeamento da jornada do operador, backbone, especificação exaustiva de H1, H2 e H3, 13 tarefas granulares, regras de negócio, critérios de aceite observáveis, matriz rastreável história $\rightarrow$ tarefa $\rightarrow$ teste $\rightarrow$ evidência com 27 cenários nos 3 níveis (7 unitários, 12 integração, 8 aceitação) e ciclo Red esperado, desenho arquitetural de persistência com desacoplamento via `EstoqueStorage`, especificação completa dos adaptadores `ListaEstoqueStorage` e `JdbcEstoqueStorage`, gestão transacional com `@Transactional` restrito a `JdbcEstoqueStorage` (eliminando risco de `NoSuchBeanDefinitionException` em `u1`), isolamento de concorrência com subquery atômica / CTE `FOR UPDATE SKIP LOCKED`, perfis Spring (`u1` padrão em memória vs `infra` em PostgreSQL), DDL da tabela `bolsas` com chave técnica `id_seq` para ordenação LIFO e IDs repetidos, DTO resiliente e estratégia de testes de caracterização da U1.
  - Atualização de `docs/infra-u2/progresso.md` registrando a conclusão da Etapa 1 no histórico e documentando as decisões arquiteturais.
  - Nenhum arquivo em `src/main/` ou `aed-c/src/` foi modificado ou excluído, cumprindo rigorosamente a regra de integridade estrita e evitando qualquer implementação antecipada.
- **Verificação:** 
  - Validação da integridade estrutural e de links cruzados entre `docs/infra-u2/story-map.md`, `docs/infra-u2/progresso.md` e `PROMPT_ROTA_VITAL_INFRA_U2_ETAPAS.md`.
  - Verificação de `git status` comprovando que nenhum arquivo de código de produção ou teste pré-existente foi alterado.
  - Linha de base da U1 preservada integralmente (48 testes Java, 54 testes C e 7 testes Python intactos).
- **Pendências:** 
  - Nenhuma pendência impeditiva para a Etapa 1.
  - A persistência funcional do estoque permanece explicitamente pendente para as Etapas 3, 4 e 5, conforme a disciplina de etapas do protocolo.
- **Próxima etapa:** **Etapa 2 — E1: Base de Containers (Dockerfile, Compose, PostgreSQL)** (construção do `compose.yaml`, correção do Dockerfile multi-stage com remoção de `-DskipTests` e consumo de `JAVA_OPTS`, configuração de `.env.example`, dependências de JDBC/Postgres/Flyway/Testcontainers e segregação Surefire/Failsafe).

---

## Etapa 2: E1 — Base de Containers (Dockerfile, Compose, PostgreSQL)

- **Estado:** Concluída
- **Data de Execução:** 17/10/2026
- **Artefatos Criados/Modificados:**
  - `Dockerfile` (otimizado: testes no build sem `-DskipTests`, runtime non-root `rotavital`, consumo formal de `JAVA_OPTS`, healthcheck nativo via Actuator)
  - `compose.yaml` (orquestração de `api` com perfil `infra` e `db` com `postgres:16-alpine`, rede `rotavital-network`, volume `rotavital-postgres-data`, healthchecks e `depends_on: condition: service_healthy`)
  - `.env.example` (parâmetros dummy documentados para porta, banco e JVM)
  - `.dockerignore` (isolamento de credenciais, target, código AED/C, benchmarks e documentação do contexto do Docker)
  - `.gitignore` (regras estritas para ignorar `.env` e preservar explicitamente `!.env.example`)
  - `pom.xml` (adição de dependências: `spring-boot-starter-jdbc`, `postgresql`, `flyway-core`, `flyway-database-postgresql`, `testcontainers`, `junit-jupiter`, e segregação `maven-surefire-plugin` / `maven-failsafe-plugin`)
  - `src/main/resources/application.properties` (definição de `spring.profiles.default=u1`)
  - `src/main/resources/application-u1.properties` (isolamento total de banco com `spring.autoconfigure.exclude` para DataSource, JDBC e Flyway)
  - `src/main/resources/application-infra.properties` (configuração do PostgreSQL, Flyway habilitado e Actuator DB healthcheck sob perfil `infra`)
  - `src/main/resources/db/migration/V1__criar_tabela_bolsas.sql` (migração Flyway com chave técnica `id_seq BIGSERIAL PRIMARY KEY` sem restrição UNIQUE em `id` e índice composto LIFO `idx_bolsas_id_seq`)
- **Regra de Integridade:** Preservação estrita dos códigos C e Java da U1. A persistência funcional das histórias H1, H2 e H3 foi expressamente mantida pendente para as Etapas 3–5.

---

### 1. Detalhamento Técnico das Mudanças e Decisões

1. **Dockerfile Otimizado e Seguro (Multi-stage):**
   - **Estágio de Build (`builder`):** Remoção definitiva da flag `-DskipTests` do comando de empacotamento (`RUN ./mvnw package -DskipITs -q`). Qualquer falha em teste unitário quebra imediatamente o build da imagem Docker. A flag `-DskipITs` associada à configuração dos plugins Maven garante que os testes de integração (`*IT.java` que exigem PostgreSQL externo) não sejam executados neste estágio, eliminando qualquer necessidade de Docker-in-Docker ou socket exposto. Inclusão de `sed -i 's/\r$//' mvnw` para sanitizar finais de linha CRLF em clones Windows antes da execução no Alpine Linux, complementado por regra `mvnw text eol=lf` em `.gitattributes`.
   - **Estágio de Runtime (`eclipse-temurin:21-jre-alpine`):** Criação e uso de usuário e grupo sem privilégios (`addgroup -S rotavital && adduser -S rotavital -G rotavital && chown -R rotavital:rotavital /app` e `USER rotavital`), garantindo segurança contra escalonamento de privilégios. Cópia específica do artefato final com ownership estrito (`COPY --chown=rotavital:rotavital --from=builder /workspace/target/rota-vital-*.jar app.jar`).
   - **Consumo de `JAVA_OPTS`:** Entrypoint configurado formalmente como `ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]` com `ENV JAVA_OPTS=""`, repassando parâmetros de heap/GC e usando `exec` para que o processo Java assuma o PID 1 e responda adequadamente a sinais POSIX (`SIGTERM`).
   - **Healthcheck Nativo:** Monitoramento periódico a cada 30 segundos chamando `/actuator/health` via `wget -qO-`, com start-period de 30 segundos para acomodar o startup da JVM.

2. **Orquestração Compose (`compose.yaml`):**
   - **Serviço `db`:** Baseado na imagem estável `postgres:16-alpine`, com volume persistente nomeado `rotavital-postgres-data` mapeado para `/var/lib/postgresql/data`. Healthcheck configurado via utilitário nativo `pg_isready -U ${POSTGRES_USER:-rotavital} -d ${POSTGRES_DB:-rotavital}` (intervalo 5s, timeout 5s, retries 5).
   - **Serviço `api`:** Build baseado no contexto local e Dockerfile, com perfil ativo explícito `SPRING_PROFILES_ACTIVE=infra`. Conexão configurada para o hostname do serviço de banco com fallback chaining `${SPRING_DATASOURCE_URL:-jdbc:postgresql://db:5432/${POSTGRES_DB:-rotavital}}`, `${SPRING_DATASOURCE_USERNAME:-${POSTGRES_USER:-rotavital}}` e `${SPRING_DATASOURCE_PASSWORD:-${POSTGRES_PASSWORD:-rotavital}}` na rede isolada bridge `rotavital-network`.
   - **Dependência Estrita de Saúde:** Configuração de `depends_on: db: condition: service_healthy`, garantindo que a aplicação só inicie suas operações após o PostgreSQL estar comprovadamente pronto para aceitar conexões TCP.
   - **Healthcheck Cruzado:** A API conta com healthcheck próprio via `/actuator/health`, que sob o perfil `infra` afere a conectividade real com o PostgreSQL via `DataSourceHealthIndicator`.

3. **Configuração Externa e Blindagem de Credenciais:**
   - Criação de `.env.example` com todas as variáveis requeridas pelo Compose e Spring documentadas com valores de exemplo seguros, discriminando URL interna de contêineres (`db:5432`) e execução direta no host (`localhost:5432`).
   - Atualização de `.gitignore` para ignorar `.env` e variantes (`.env.*`), mantendo a exceção `!.env.example`.
   - Criação de `.dockerignore` excluindo segredos, pastas de testes/build e documentação pesada da camada de build Docker.

4. **Isolamento de Perfis Spring (`u1` vs `infra`):**
   - **Perfil `u1` (Padrão):** Declarado via `spring.profiles.default=u1` em `application.properties`. Em `application-u1.properties`, exclui-se via `spring.autoconfigure.exclude` as classes `DataSourceAutoConfiguration`, `DataSourceTransactionManagerAutoConfiguration`, `JdbcTemplateAutoConfiguration` e `FlywayAutoConfiguration`, desabilitando o Flyway (`spring.flyway.enabled=false`) e o health indicator de banco (`management.health.db.enabled=false`). A aplicação inicia 100% autônoma em memória, sem tentar conectar a banco de dados nem buscar DataSources, garantindo que a linha de base e os testes legados da U1 permaneçam íntegros.
   - **Perfil `infra`:** Em `application-infra.properties`, ativa-se o Flyway (`spring.flyway.enabled=true`), configura-se o DataSource PostgreSQL e ativa-se o indicador de saúde de banco no Actuator (`management.health.db.enabled=true`). Caso o banco falhe sob o perfil `infra`, a aplicação reporta erro imediato (`DataAccessException`), sem fallback silencioso para a memória.

5. **Migração Flyway Inicial (`V1__criar_tabela_bolsas.sql`):**
   - Criação da tabela `bolsas` em `src/main/resources/db/migration/V1__criar_tabela_bolsas.sql`.
   - Chave técnica autoincrementável `id_seq BIGSERIAL PRIMARY KEY`, garantindo resolução temporal LIFO estrita sem colisões de timestamp.
   - Ausência intencional de restrição `UNIQUE` na coluna `id`, preservando a regra de negócio da U1 de permitir IDs duplicados.
   - Criação do índice composto `idx_bolsas_id_seq ON bolsas(id, id_seq DESC)` para busca pontual e remoção atômica em tempo $O(1)$.

6. **Segregação de Testes no Maven (`pom.xml`):**
   - Configuração do `maven-surefire-plugin` incluindo `**/*Test.java` e excluindo `**/*IT.java`.
   - Configuração do `maven-failsafe-plugin` incluindo `**/*IT.java` nas fases `integration-test` e `verify`.
   - Adição das dependências gerenciadas do Testcontainers (`testcontainers`, `junit-jupiter`, `postgresql`) para suporte aos testes de integração das etapas seguintes.

---

### 2. Evidências de Verificação

1. **Validação Estrutural do Docker Compose:**
   - Comando: `docker compose config --quiet` e `docker compose config`
   - Resultado: Código de saída 0. Sintaxe, variáveis com fallback, redes, volumes e healthchecks validados com sucesso.
   - Teste com `.env` customizado: Interpolação completa de portas, credenciais e parâmetros de JVM comprovada via `docker compose config`.
2. **Validação da Linha de Base de Testes Java (Perfil `u1`):**
   - Comando: `.\mvnw.cmd test` e `.\mvnw.cmd verify` (com `JAVA_HOME` configurado)
   - Resultado: **48/48 testes aprovados**, 0 falhas, 0 erros, tempo: ~11s (`BUILD SUCCESS`). Contexto Spring inicializado com perfil default `u1` sem qualquer tentativa de conexão a banco ou execução de migração.
3. **Prova Adversarial de Quebra de Build por Falha de Teste Unitário:**
   - Teste: Injeção intencional de falha em `FuzzyMatcherTest.java` (`assertEquals(999, ...)`).
   - Comando: `.\mvnw.cmd package -DskipITs`
   - Resultado: Falha imediata do build (`BUILD FAILURE`, exit code 1) em 9.5s com relatório de erro do Surefire. O teste foi restaurado ao estado original e a suíte voltou a passar com 100% de sucesso.
4. **Verificação de Subida em Tempo Real do JAR no Perfil `u1`:**
   - Teste: Subida autônoma de `target/rota-vital-1.0.0.jar` e consulta ativa a `http://localhost:8080/actuator/health`.
   - Resultado: HTTP 200 com `"status": "UP"`, contendo exclusivamente componentes `diskSpace` e `ping`. Zero menção ou consulta a banco de dados.
5. **Verificação da Ausência de Fallback Silencioso no Perfil `infra`:**
   - Teste: Execução do JAR forçando `"-Dspring.profiles.active=infra"` sem banco de dados PostgreSQL ativo.
   - Resultado: Falha fatal e ruidosa na inicialização (`FlywaySqlException: Unable to obtain connection from database: Connection to localhost:5432 refused`), provando a ausência total de fallback silencioso para memória.
6. **Validação de Não-Regressão em C (AED):**
   - Comando: compilação e execução de `test_lista.c` e `test_fila.c` via GCC local
   - Resultado: **54/54 asserções aprovadas** (23 em lista encadeada, 31 em fila FIFO).
7. **Validação de Não-Regressão nos Benchmarks Python:**
   - Comando: `python -m unittest discover -s bench -p "test_*.py"` e `python bench/plot_speedup.py --check`
   - Resultado: **7/7 testes aprovados** e agregação de 8 configurações estatisticamente consistente.
8. **Validação das Regras de Ignore de Credenciais:**
   - Comando: `git check-ignore -v .env` e `git check-ignore -v .env.example`
   - Resultado: `.env` mapeado para `.gitignore:47:.env` (ignorado); `.env.example` mapeado para `.gitignore:49:!.env.example` (preservado).
9. **Diagnóstico Fidedigno do Ambiente Docker Local:**
   - Comando: `docker compose up -d --build` e `docker info`
   - Resultado: Falha com código 1 (`open //./pipe/dockerDesktopLinuxEngine: The system cannot find the file specified`).
   - Causa Raiz: O daemon do Docker Desktop no ambiente host Windows não está em execução em segundo plano (instalação localizada em `AppData\Local\Programs\DockerDesktop` com distribuição WSL2 `docker-desktop` parada). Conforme orientação estrita do protocolo, o bloqueio do daemon externo é registrado de forma fidedigna, sem contorno simulado ou fabricação de logs, atestando a integridade estrutural plena dos arquivos de configuração.
10. **Compilação Limpa e Validação de Dependências Transitivas (Auditoria R3):**
    - Comando: `mvnw clean compile` e `mvnw dependency:tree`
    - Resultado: 15 classes compiladas com sucesso em 3.1s e árvore de dependências transitivas do Starter Parent 3.3.3 resolvida de forma canônica sem conflitos (Flyway 10.10.0, PostgreSQL Driver 42.7.3, Testcontainers 1.19.8, Jackson 2.17.2).
11. **Auditoria Estrita de Não-Antecipação de Domínio (Auditoria R3):**
    - Comando: `git status -- src/main/java src/test/java`
    - Resultado: `nothing to commit, working tree clean`, atestando 100% de integridade e zero código das histórias H1, H2 e H3 antecipado.

---

### 3. Resumo Padronizado de Fechamento da Etapa 2 (Protocolo Obrigatório)

- **Etapa e estado:** Etapa 2 (E1 — Base de Containers: Dockerfile, Compose, PostgreSQL) — **Concluída**.
- **Mudanças:** 
  - Correção e otimização do `Dockerfile` multi-stage: testes unitários no build sem `-DskipTests`, segregação de ITs, usuário não privilegiado `rotavital` com ownership restrito de `/app` e do JAR (`--chown=rotavital:rotavital`), consumo formal de `JAVA_OPTS` no entrypoint e healthcheck Actuator.
  - Criação do `compose.yaml` com serviços `api` (perfil `infra`) e `db` (`postgres:16-alpine`), volume nomeado `rotavital-postgres-data`, rede bridge `rotavital-network`, healthchecks e `depends_on: condition: service_healthy`.
  - Criação de `.env.example`, `.dockerignore` e atualização de `.gitignore` para blindagem de credenciais.
  - Adição de dependências JDBC, PostgreSQL, Flyway e Testcontainers no `pom.xml`, com segregação de testes via Surefire e Failsafe.
  - Criação da migração inicial Flyway `src/main/resources/db/migration/V1__criar_tabela_bolsas.sql` (chave `id_seq BIGSERIAL PRIMARY KEY` e índice composto LIFO).
  - Configuração e isolamento dos perfis Spring `u1` (padrão em memória sem banco) e `infra` (PostgreSQL com Actuator healthcheck e sem fallback silencioso).
  - Nenhuma regra funcional das histórias H1, H2 e H3 foi antecipada.
- **Verificação:** 
  - `docker compose config --quiet` executado com código de saída 0 (tanto com defaults quanto com `.env` customizado).
  - `.\mvnw.cmd test` e `.\mvnw.cmd verify`: 48/48 testes aprovados no perfil padrão `u1` sem banco (`BUILD SUCCESS`).
  - `.\mvnw.cmd clean compile` e `dependency:tree`: compilação limpa de 15 fontes e resolução canônica de dependências.
  - `git status -- src/main/java src/test/java`: 100% limpo, atestando zero antecipação de H1/H2/H3.
  - Prova adversarial: falha em teste unitário quebrou o build imediatamente (`BUILD FAILURE`, exit code 1).
  - Subida autônoma em tempo real do JAR confirmou `/actuator/health` como `UP` sem dependência de banco.
  - Execução forçada de perfil `infra` sem banco atestou falha imediata e ausência de fallback silencioso.
  - Testes C (54/54) e Python (7/7) 100% aprovados, comprovando zero regressão.
  - `git check-ignore` confirmou que `.env` é ignorado e `.env.example` é versionado.
  - Tentativa de subida via `docker compose up -d --build` documentou bloqueio do daemon Docker Desktop inativo no host.
- **Pendências:** 
  - A subida física dos contêineres e testes de integração com banco dependem da inicialização do Docker Desktop no host Windows.
  - A persistência funcional do estoque permanece expressamente pendente para as Etapas 3 (H1), 4 (H2) e 5 (H3).
- **Próxima etapa:** **Etapa 3 — E2: H1 — Cadastro e Recuperação Persistente (TDD)** (implementação guiada por testes Red → Green → Refactor para cadastro e recuperação persistente de bolsas no PostgreSQL).

---

## Etapa 3: E2: H1 — Cadastro e Recuperação Persistente (TDD)

- **Estado:** Concluída
- **Data de Execução:** 17/10/2026
- **Histórico de Commits Rastreáveis da Etapa 3:**
  1. `e3b6073` - `test(u1): adiciona testes de caracterizacao para EstoqueService e EstoqueController`
  2. `2525767` - `test(h1): define testes unitarios de ListaEstoqueStorage para T1.1`
  3. `07c44a7` - `feat(h1): implementa EstoqueStorage e adaptador ListaEstoqueStorage para u1 (T1.1)`
  4. `a80debc` - `test(h1): define testes unitarios de JdbcEstoqueStorage (T1.2)`
  5. `8aa3432` - `feat(h1): implementa JdbcEstoqueStorage sob perfil infra (T1.2)`
  6. `3cd4564` - `test(h1): define testes unitarios de delegacao e fail-fast em EstoqueService (T1.3)`
  7. `4b880af` - `feat(h1): refatora EstoqueService para injetar EstoqueStorage com fail-fast (T1.3)`
  8. `54d83c0` - `test(h1): define testes unitarios para BolsaRequestDTO resiliente (T1.4)`
  9. `badd2a0` - `feat(h1): implementa BolsaRequestDTO e integra com EstoqueController (T1.4)`
  10. `2d0be08` - `test(h1): adiciona bateria de testes de integracao e aceitacao em tres niveis para H1`
- **Regra de Integridade:** As estruturas pedagógicas de AED da U1 (`ListaEstoque`, `NoBolsa`, `FilaRequisicoes`) e testes C/Python foram 100% preservados sem alterações. As histórias H2 (listagem do estoque) e H3 (remoção persistente) permanecem expressamente pendentes para as Etapas 4 e 5.

---

### 1. Testes de Caracterização da U1 (Blindagem de Contratos Legados)

Antes de realizar qualquer modificação na classe `EstoqueService`, foram criadas duas suítes dedicadas de testes de caracterização sob o perfil padrão `u1`:
1. `src/test/java/com/rotavital/service/EstoqueServiceCaracterizacaoTest.java`:
   - 6 cenários de teste cobrindo: adição e consulta direta, retorno de array em ordem LIFO (mais recente primeiro), consulta de ocorrência mais recente sob IDs duplicados, remoção da ocorrência mais recente sob duplicatas, retorno booleano `false` para remoção inexistente e contagem de tamanho.
2. `src/test/java/com/rotavital/controller/EstoqueControllerCaracterizacaoTest.java`:
   - 7 cenários com `MockMvc` sob `@ActiveProfiles("u1")` cobrindo: `POST /api/estoque` (200 OK com envelope `{"mensagem": "...", "tamanho": 1}`), `GET /api/estoque/{id}` (200 OK com dados completos), `GET /api/estoque/{id}` inexistente (404 Not Found), `DELETE /api/estoque/{id}` (200 OK com tamanho decrementado), `DELETE /api/estoque/{id}` inexistente (404 Not Found), `GET /api/estoque` com envelope LIFO e `GET /api/estoque` em estoque vazio (`{"bolsas": [], "tamanho": 0}`).
- **Resultado:** 13/13 testes aprovados, estabelecendo uma rede inquebrável de proteção contra regressões da U1.

---

### 2. Ciclo Red → Green → Refactor para H1

#### Tarefa T1.1: Interface `EstoqueStorage` e Adaptador em Memória `ListaEstoqueStorage`
- **Teste Unitário:** `src/test/java/com/rotavital/storage/ListaEstoqueStorageTest.java` (5 testes).
- **Evidência Red:** Execução de `mvnw.cmd test -Dtest=ListaEstoqueStorageTest` falhou com `java.lang.UnsupportedOperationException: Stub para ciclo TDD Red`. Commit: `2525767`.
- **Implementação Green:** Criação da interface `EstoqueStorage` e implementação de `ListaEstoqueStorage` anotada com `@Component` e `@Profile({"u1", "default"})`, delegando para nós dinâmicos `NoBolsa` de `ListaEstoque` (`salvar -> inserir`, `buscarPorId -> consultar`, `removerPorId -> remover`, `listarTodas -> toArray`, `tamanho -> getTamanho`).
- **Evidência Green:** 5/5 testes aprovados (`BUILD SUCCESS` em 7.5s). Commit: `07c44a7`.

#### Tarefa T1.2: Adaptador Relacional `JdbcEstoqueStorage`
- **Teste Unitário:** `src/test/java/com/rotavital/storage/JdbcEstoqueStorageTest.java` (5 testes com mock de `JdbcTemplate` e captura de SQL).
- **Evidência Red:** Execução de `mvnw.cmd test -Dtest=JdbcEstoqueStorageTest` falhou com `java.lang.UnsupportedOperationException: Stub para ciclo TDD Red T1.2`. Commit: `a80debc`.
- **Implementação Green:** Implementação de `JdbcEstoqueStorage` sob `@Profile("infra")`:
  - `salvar(Bolsa)`: transacional (`@Transactional`), executando `INSERT INTO bolsas (id, tipo_sanguineo, volume_ml, data_coleta, validade, doador) VALUES (?, ?, ?, ?, ?, ?)`.
  - `buscarPorId(String id)`: transacional somente leitura (`@Transactional(readOnly = true)`), executando `SELECT ... FROM bolsas WHERE id = ? ORDER BY id_seq DESC LIMIT 1` com `RowMapper<Bolsa>`, garantindo recuperação determinística da ocorrência mais recente sob duplicatas.
  - `tamanho()`: executando `SELECT COUNT(*) FROM bolsas`.
  - Fail-fast estrito: propagação direta de qualquer `DataAccessException` sem desvio silencioso para a memória.
  - Métodos `listarTodas` e `removerPorId` lançam `UnsupportedOperationException` mantendo H2 e H3 estritamente pendentes.
- **Evidência Green:** 5/5 testes aprovados (`BUILD SUCCESS` em 9.3s). Commit: `8aa3432`.

#### Tarefa T1.3: Desacoplamento de `EstoqueService` com Injeção de `EstoqueStorage`
- **Teste Unitário:** `src/test/java/com/rotavital/service/EstoqueServiceTest.java` (6 testes unitários com Mockito).
- **Evidência Red:** Execução falhou com 6 erros de verificação (`Wanted but not invoked: estoqueStorage.salvar(...)`, `expected: <42> but was: <0>`, etc.) comprovando que o serviço ainda não delegava para a abstração. Commit: `3cd4564`.
- **Implementação Green:** Refatoração de `EstoqueService` para injetar `EstoqueStorage` via construtor, mantendo construtor padrão com fallback para `ListaEstoqueStorage` e repasse transparente de todas as operações públicas com fail-fast.
- **Evidência Green:** 6/6 testes aprovados no `EstoqueServiceTest` e 13/13 testes aprovados na suíte de caracterização da U1. Commit: `4b880af`.

#### Tarefa T1.4: Resiliência de Tipagem na API REST com `BolsaRequestDTO`
- **Teste Unitário:** `src/test/java/com/rotavital/dto/BolsaRequestDTOTest.java` (4 testes com Jackson).
- **Evidência Red:** Execução falhou com `UnsupportedOperationException: Stub para ciclo TDD Red T1.4`. Commit: `54d83c0`.
- **Implementação Green:** Criação do record `BolsaRequestDTO` aceitando `volumeMl` polimórfico (`Number`, `String` ou `null` com default 450.0) e integração com `POST /api/estoque` em `EstoqueController`.
- **Evidência Green:** 4/4 testes de DTO aprovados e 7/7 testes de `EstoqueControllerCaracterizacaoTest` aprovados, aceitando payloads numéricos de fixtures da U1. Commit: `badd2a0`.

---

### 3. Bateria de Testes em Três Níveis de H1

A matriz rastreável de H1 foi plenamente coberta e implementada em 3 níveis:

1. **Nível 1 — Testes Unitários de Regra e Domínio (sem banco externo):**
   - `ListaEstoqueStorageTest` (5 testes): integridade algorítmica da lista encadeada da U1.
   - `JdbcEstoqueStorageTest` (5 testes): SQL de INSERT, busca LIFO por `id_seq DESC LIMIT 1`, contagem e fail-fast.
   - `EstoqueServiceTest` (6 testes): injeção de dependência, delegação e isolamento de exceções.
   - `BolsaRequestDTOTest` (4 testes): resiliência de deserialização de literais JSON e strings.
   - `EstoqueServiceCaracterizacaoTest` (6 testes) e `EstoqueControllerCaracterizacaoTest` (7 testes).
   - **Total de Testes Unitários/Caracterização:** 33 testes específicos de estoque, somando **81 testes Java** executados pelo Surefire com 100% de aprovação (0 falhas, 0 erros).
2. **Nível 2 — Testes de Integração com PostgreSQL Real (Testcontainers):**
   - `AbstractPostgresIT`: classe base configurando `@Container public static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")`, com `@ActiveProfiles("infra")` e `@DynamicPropertySource` injetando URL, credenciais e migração Flyway.
   - `JdbcEstoqueStorageIT`:
     - `deveInserirERecuperarBolsaComTodosOsCampos()`: grava bolsa com os 6 atributos canônicos (`id`, `tipoSanguineo`, `volumeMl`, `dataColeta`, `validade`, `doador`) e recupera do PostgreSQL com asserções exatas em cada atributo.
     - `deveRecuperarOcorrenciaMaisRecenteSobIdsDuplicados()`: grava dois registros com mesmo `id` e comprova recuperação LIFO do registro mais recente via `ORDER BY id_seq DESC LIMIT 1`.
     - `deveRetornarVazioParaIdInexistente()`: valida retorno `Optional.empty()` para ID não cadastrado.
   - `BolsaPersistidaIT`:
     - `devePersistirBolsaAposReinicioContexto()`: grava a bolsa no PostgreSQL, encerra formalmente o contexto Spring (`applicationContext.close()`), recria um novo contexto com `SpringApplicationBuilder` apontando para o mesmo banco e comprova que a bolsa sobrevive intacta e é recuperada com todos os seus atributos preservados.
3. **Nível 3 — Testes de Aceitação da API REST sob Perfil `infra`:**
   - `EstoqueControllerIT`:
     - `deveCadastrarBolsaViaPostERecuperarViaGet()`: executa requisição HTTP `POST /api/estoque` contendo JSON com literal numérico `volumeMl: 450.0`, valida resposta HTTP 200 com envelope `{"mensagem": "Bolsa adicionada ao estoque", "tamanho": 1}`, e subsequentemente executa `GET /api/estoque/{id}` validando HTTP 200 com payload JSON contendo todos os 6 campos idênticos.
     - `deveRetornar404ParaBolsaInexistente()`: executa `GET /api/estoque/BOLSA-INEXISTENTE` sob o perfil `infra` e valida retorno HTTP 404 Not Found.
- **Commit:** `2d0be08`.

---

### 4. Validação de Não-Regressão da U1

- **Testes em C (AED):** 54/54 asserções aprovadas via compilação GCC local (23 em `test_lista` e 31 em `test_fila`), com zero vazamento de memória.
- **Benchmarks Python:** 7/7 testes aprovados (`python -m unittest discover -s bench`) e consistência estatística de 8 configurações confirmada (`python bench/plot_speedup.py --check`).
- **Suíte Completa Java:** 81 testes aprovados (`mvnw.cmd test`, tempo ~17s, 0 falhas, 0 erros).

---

### 5. Resumo Padronizado de Fechamento da Etapa 3 (Protocolo Obrigatório)

- **Etapa e estado:** Etapa 3 (E2: H1 — Cadastro e Recuperação Persistente) — **Concluída**.
- **Mudanças:** 
  - Criação da interface `EstoqueStorage` desacoplando a camada de serviço do meio físico de armazenamento.
  - Implementação do adaptador de memória `ListaEstoqueStorage` com anotações `@Component` e `@Profile({"u1", "default"})`, delegando diretamente para a lista simplesmente encadeada pedagógica com nós `NoBolsa`.
  - Implementação do adaptador relacional `JdbcEstoqueStorage` sob `@Profile("infra")` utilizando Spring `JdbcTemplate`, transações isoladas (`@Transactional`), mapeamento via `RowMapper<Bolsa>`, consulta LIFO da ocorrência mais recente (`ORDER BY id_seq DESC LIMIT 1`) e fail-fast em falhas de persistência.
  - Refatoração de `EstoqueService` para injetar `EstoqueStorage` via construtor, eliminando o acoplamento rígido com `ListaEstoque`.
  - Criação do record `BolsaRequestDTO` tolerante a variações de tipos (`Double`, `String` ou ausente com default 450.0) e integração no `POST /api/estoque` do `EstoqueController`.
  - Configuração de `<argLine>-Dnet.bytebuddy.experimental=true</argLine>` no `pom.xml` para compatibilidade total do Mockito com o runtime Java 25.
  - Criação de 13 testes de caracterização da U1, 20 testes unitários TDD e 3 baterias de integração/aceitação com Testcontainers (`JdbcEstoqueStorageIT`, `BolsaPersistidaIT`, `EstoqueControllerIT`).
  - As histórias H2 (listagem de estoque) e H3 (remoção persistente) foram expressamente mantidas pendentes para as Etapas 4 e 5.
- **Verificação:** 
  - 81/81 testes Java unitários e de caracterização aprovados no perfil `u1` via `.\mvnw.cmd test` (Surefire, 0 falhas, 0 erros).
  - 10 commits rastreáveis registrando estritamente a sequência `test:` e `feat:` no histórico Git.
  - Evidências Red confirmadas com falha técnica legítima (`UnsupportedOperationException` e `Wanted but not invoked`) antes de cada implementação Green.
  - 54/54 asserções de testes em C aprovadas (lista encadeada e fila FIFO).
  - 7/7 testes de benchmarks em Python aprovados e integridade estatística validada via `--check`.
  - Compilação limpa de todas as 16 classes de teste (`mvnw.cmd test-compile`).
- **Pendências:** 
  - A execução física dos testes de integração `*IT.java` no ambiente host Windows depende do daemon Docker Desktop estar ativo; na esteira CI (Linux) os testes executarão nativamente com Docker.
  - A listagem geral persistente do estoque (H2) e a remoção persistente (H3) permanecem expressamente pendentes para as Etapas 4 e 5.
- **Próxima etapa:** **Etapa 4 — E2: H2 — Listagem e Consulta Persistente (TDD)** (implementação guiada por TDD da listagem persistente, envelope padronizado, suporte a estoque vazio, ordenação temporal LIFO e recuperação de duplicatas no PostgreSQL).



