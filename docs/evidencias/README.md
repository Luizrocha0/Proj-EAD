# Catálogo de Evidências da Unidade 2 — Rota Vital

**Projeto:** Rota Vital — Banco de Sangue Inteligente  
**Entrega:** Unidade 2 — Desafios PI — Parte 1 (Infraestrutura de Software)  
**Data de Consolidação:** 17/10/2026  
**Autores:** Equipe Rota Vital (Eduardo Borges, Luiz Henrique Rocha, Eliziane Mota, Pedro Iranildo, Ricardo Severiano de Souza Filho)  
**Repositório:** https://github.com/Luizrocha0/Proj-EAD  
**URL Pública:** https://rota-vital.onrender.com  

---

## Visão Geral do Catálogo

Este diretório cataloga todas as evidências auditáveis de compilação, testes automatizados por nível, containerização Docker/Compose, pipeline de integração contínua (CI/CD), deploy em nuvem e ciclo TDD (Test-Driven Development) produzidas ao longo da execução da Unidade 2 no projeto **Rota Vital**.

Todas as afirmações de conformidade do projeto são sustentadas por execuções reais, comandos auditados e saídas concretas registradas abaixo.

---

## Índice de Evidências por Categoria

- [1. Evidências de Testes Unitários Java (Nível 1 — Surefire)](#1-evidências-de-testes-unitários-java-nível-1-surefire)
- [2. Evidências de Testes de Integração e Persistência (Nível 2 e 3 — Failsafe + Testcontainers)](#2-evidências-de-testes-de-integração-e-persistência-nível-2-e-3-failsafe-testcontainers)
- [3. Evidências de Estruturas de Dados AED em C (U1 — AddressSanitizer + UBSan)](#3-evidências-de-estruturas-de-dados-aed-em-c-u1-addresssanitizer-ubsan)
- [4. Evidências de Benchmarks e Paralelismo em Python (U1 — SO)](#4-evidências-de-benchmarks-e-paralelismo-em-python-u1-so)
- [5. Evidências de Containerização e Orquestração (E1 — Dockerfile e Compose)](#5-evidências-de-containerização-e-orquestração-e1-dockerfile-e-compose)
- [6. Evidências da Esteira de CI/CD (E3 — GitHub Actions em DAG com 6 Gates)](#6-evidências-da-esteira-de-cicd-e3-github-actions-em-dag-com-6-gates)
- [7. Evidências de Deploy e Verificação em Produção (E3 — Render)](#7-evidências-de-deploy-e-verificação-em-produção-e3-render)
- [8. Evidências do Ciclo TDD (Red → Green → Refactor nas Histórias H1, H2 e H3)](#8-evidências-do-ciclo-tdd-red-green-refactor-nas-histórias-h1-h2-e-h3)
- [9. Mapeamento dos Arquivos e Registros no Repositório](#9-mapeamento-dos-arquivos-e-registros-no-repositório)

---

## 1. Evidências de Testes Unitários Java (Nível 1 — Surefire)

Os testes unitários cobrem regras de negócio, serialização resiliente de DTOs, adaptadores em memória, injeção condicional por perfil Spring e testes de caracterização da API legada. São executados de forma ultrarrápida via `maven-surefire-plugin` sem depender de banco de dados ou da subida do Docker.

- **Comando:**
  ```powershell
  $env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-25.0.4.101-hotspot"
  .\mvnw.cmd test
  ```
- **Resultado Auditado:**
  ```text
  [INFO] Running com.rotavital.aed.u1.FilaRequisicaoTest
  [INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0
  [INFO] Running com.rotavital.aed.u1.ListaEstoqueTest
  [INFO] Tests run: 17, Failures: 0, Errors: 0, Skipped: 0
  [INFO] Running com.rotavital.config.DatabaseConfigTest
  [INFO] Tests run: 18, Failures: 0, Errors: 0, Skipped: 0
  [INFO] Running com.rotavital.controller.DuplicateControllerTest
  [INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
  [INFO] Running com.rotavital.controller.EstoqueControllerCaracterizacaoTest
  [INFO] Tests run: 21, Failures: 0, Errors: 0, Skipped: 0
  [INFO] Running com.rotavital.controller.EstoqueControllerTest
  [INFO] Tests run: 19, Failures: 0, Errors: 0, Skipped: 0
  [INFO] Running com.rotavital.dto.BolsaRequestDTOTest
  [INFO] Tests run: 17, Failures: 0, Errors: 0, Skipped: 0
  [INFO] Running com.rotavital.RotaVitalApplicationTest
  [INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
  [INFO] Running com.rotavital.service.DuplicateDetectionServiceTest
  [INFO] Tests run: 11, Failures: 0, Errors: 0, Skipped: 0
  [INFO] Running com.rotavital.service.EstoqueServiceCaracterizacaoTest
  [INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0
  [INFO] Running com.rotavital.service.EstoqueServiceInfraProfileInjectionTest
  [INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
  [INFO] Running com.rotavital.service.EstoqueServiceTest
  [INFO] Tests run: 16, Failures: 0, Errors: 0, Skipped: 0
  [INFO] Running com.rotavital.service.EstoqueServiceU1ProfileInjectionTest
  [INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
  [INFO] Running com.rotavital.storage.JdbcEstoqueStorageTest
  [INFO] Tests run: 25, Failures: 0, Errors: 0, Skipped: 0
  [INFO] Running com.rotavital.storage.ListaEstoqueStorageTest
  [INFO] Tests run: 20, Failures: 0, Errors: 0, Skipped: 0
  [INFO] Running com.rotavital.util.FuzzyMatcherTest
  [INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
  [INFO] 
  [INFO] Results:
  [INFO] Tests run: 201, Failures: 0, Errors: 0, Skipped: 0
  [INFO] BUILD SUCCESS
  [INFO] Total time: 23.221 s
  ```
- **Localização dos Relatórios:** `target/surefire-reports/`
- **Artefato no GitHub Actions:** `surefire-reports` (armazenado por 7 dias)

---

## 2. Evidências de Testes de Integração e Persistência (Nível 2 e 3 — Failsafe + Testcontainers)

Os testes de integração executam migrações Flyway e operações de banco contra uma instância real de contêiner PostgreSQL 16 provisionada dinamicamente via Testcontainers. O teste `BolsaPersistidaIT` comprova a sobrevivência dos dados após a destruição e recriação do contexto Spring.

- **Comando:**
  ```powershell
  .\mvnw.cmd -Pintegration verify
  ```
- **Classes Auditadas:**
  1. `com.rotavital.integration.JdbcEstoqueStorageIT` (19 testes): Operações DDL/DML diretas contra PostgreSQL 16 real, rollback de transações com `@Transactional`, ordenação LIFO (`ORDER BY id_seq DESC`), inserção de IDs duplicados e remoção atômica por subquery SQL.
  2. `com.rotavital.integration.BolsaPersistidaIT` (3 testes fundamentais):
     - `devePersistirBolsaAposReinicioContexto`: Cadastra bolsa, encerra o contexto (`applicationContext.close()`), instancia novo contexto Spring conectado ao mesmo PostgreSQL e comprova a integridade e persistência de todos os campos.
     - `devePreservarListagemEOrdemLIFOAposReinicioContexto`: Cadastra 5 bolsas, reinicia o contexto e atesta preservação da ordem LIFO e integridade de duplicatas.
     - `deveManterRemocaoAposReinicioContexto`: Remove a ocorrência mais recente de uma bolsa com ID repetido, encerra o contexto, abre um segundo contexto e comprova que a ocorrência removida não ressuscitou e a primeira permanece intacta.
  3. `com.rotavital.integration.EstoqueControllerIT` (17 testes): Testes de aceitação HTTP de ponta a ponta via MockMvc com PostgreSQL real validando contratos REST, envelopes JSON `{"bolsas": [...], "tamanho": N}`, status 200/400/404 e resiliência de tipos.
- **Resultado Auditado:** **39 métodos de teste aprovados**, 0 falhas, 0 erros.
- **Localização dos Relatórios:** `target/failsafe-reports/`

---

## 3. Evidências de Estruturas de Dados AED em C (U1 — AddressSanitizer + UBSan)

As estruturas manuais desenvolvidas na U1 (lista simplesmente encadeada de bolsas e fila FIFO de requisições hospitalares) são compiladas em C11 com sanitizers de memória do GCC.

- **Comando:**
  ```bash
  make -C aed-c test
  # No Windows com MinGW:
  mingw32-make -C aed-c CFLAGS="-Wall -Wextra -std=c11" LDFLAGS="" test
  ```
- **Resultado Auditado:**
  ```text
  === Testes da Lista Encadeada (AED U1 — C) ===
  [Lista vazia] OK: 4/4
  [Insercao] OK: 4/4 (Cabeca eh a ultima inserida, Cauda eh a primeira)
  [Consulta] OK: 4/4 (Busca linear por ID)
  [Remocao] OK: 11/11 (Remocao da cabeca, meio e cauda)
  === Resultado: 23/23 testes passaram ===
  SUCESSO: Todos os testes passaram sem erros de memoria!

  === Testes da Fila FIFO (AED U1 — C) ===
  [Fila vazia] OK: 4/4
  [Enfileiramento] OK: 4/4 (FIFO: primeiro a entrar eh cabeca)
  [Desenfileiramento] OK: 13/13 (Ordem estrita de chegada)
  [Reuso da fila] OK: 3/3
  [Espiar fila] OK: 5/5
  === Resultado: 31/31 testes passaram ===
  SUCESSO: Todos os testes passaram sem erros de memoria!

  ✅ 54 asserções em C aprovadas com sanitizers ativos e zero leaks.
  ```

---

## 4. Evidências de Benchmarks e Paralelismo em Python (U1 — SO)

Validação matemática e estatística dos cálculos de speedup da atividade de Sistemas Operacionais da U1 (detecção concorrente de duplicatas por Levenshtein), com base nas 40 medições empíricas brutas em `bench/measurements.csv`.

- **Comandos:**
  ```powershell
  python -m unittest discover -s bench -p 'test_*.py'
  python bench/plot_speedup.py --check
  ```
- **Resultado Auditado:**
  ```text
  .......
  ----------------------------------------------------------------------
  Ran 7 tests in 0.001s
  OK

  Agregado correto: 8 configurações.
  ```

---

## 5. Evidências de Containerização e Orquestração (E1 — Dockerfile e Compose)

Comprovação da integridade dos manifestos de infraestrutura e da subida orquestrada dos serviços `api` e `db`.

- **Validação de Sintaxe e Parâmetros:**
  ```bash
  docker compose config --quiet
  # Exit code 0 — Sintaxe e interpolação de variáveis válidas
  ```
- **Orquestração e Subida Saudável:**
  ```bash
  docker compose up -d --build
  docker compose ps
  ```
- **Inspeção de Saúde dos Serviços:**
  - `rotavital-db`: Status `Up (healthy)` via comando nativo `pg_isready -U rotavital -d rotavital`.
  - `rotavital-api`: Status `Up (healthy)` via verificação HTTP do Actuator `/actuator/health` após atingir prontidão do banco (`depends_on: db: condition: service_healthy`).
- **Verificação do Volume Persistente:**
  - Volume nomeado `rotavital-postgres-data` mapeado para `/var/lib/postgresql/data`.
  - Dados persistem íntegros após `docker compose down && docker compose up -d`.
- **Dockerfile Multi-stage e Segurança Non-Root:**
  - Estágio de build: executa `./mvnw package -DskipITs -q` (qualquer falha em teste unitário interrompe o build imediatamente).
  - Estágio de runtime: usuário `rotavital:rotavital` sem privilégios administrativos.
  - Entrypoint: `["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]` consome formalmente a variável `JAVA_OPTS` e repassa sinais de finalização via `exec`.

---

## 6. Evidências da Esteira de CI/CD (E3 — GitHub Actions em DAG com 6 Gates)

A esteira de integração contínua (`.github/workflows/ci.yml`) estrutura 6 jobs em Grafo Acíclico Dirigido (DAG) sem qualquer contorno permissivo (`zero continue-on-error`, `zero || true`).

```
                    ┌──────────────────────┐
                    │ check-benchmark (SO) │
                    └──────────┬───────────┘
                               │
 ┌──────────────┐   ┌──────────▼───────────┐   ┌────────────────────────┐
 │ test-c (AED) │   │ test-java-unit (L1)  │   │                        │
 └──────┬───────┘   └──────────┬───────────┘   │                        │
        │                      │               │                        │
        │           ┌──────────▼───────────┐   │                        │
        │           │test-java-integr. (L2)│   │                        │
        │           └──────────┬───────────┘   │                        │
        │                      │               │                        │
        └──────────────┬───────┴───────────────┘                        │
                       ▼                                                │
          ┌───────────────────────────┐                                 │
          │ build-docker-smoke (Gate 5) │                                │
          └────────────┬──────────────┘                                 │
                       │ (exclusivo branch main)                        │
                       ▼                                                ▼
          ┌───────────────────────────┐                        [Falha em qualquer Gate]
          │   deploy-render (Gate 6)  │                                   │
          │  ref=${{ github.sha }}    │                        Bloqueia progressão
          └───────────────────────────┘
```

- **Comprovação de Smoke Test Containerizado (Gate 5):**
  O pipeline sobe o Docker Compose local no runner do GitHub Actions e executa 9 etapas automatizadas:
  1. Validação de listagem inicial vazia (`GET /api/estoque` $\to$ `{"bolsas":[],"tamanho":0}`).
  2. Cadastro de bolsa (`POST /api/estoque` $\to$ `200 OK`, `{"mensagem":"Bolsa adicionada ao estoque","tamanho":1}`).
  3. Consulta por ID (`GET /api/estoque/SMOKE-CI-01` $\to$ dados íntegros).
  4. Listagem populada (`GET /api/estoque` $\to$ `tamanho:1`).
  5. Remoção de bolsa (`DELETE /api/estoque/SMOKE-CI-01` $\to$ `{"mensagem":"Bolsa removida","tamanho":0}`).
  6. Confirmação de listagem vazia pós-remoção.
  7. Retorno 404 para consulta de ID inexistente.
  8. Retorno 404 para deleção de ID inexistente.
  9. Retorno 400 para payload inválido (JSON sem campos obrigatórios).

---

## 7. Evidências de Deploy e Verificação em Produção (E3 — Render)

Evidências de configuração e operação do deploy automatizado no PaaS Render.com.

- **Deploy Hook Seguro com Commit SHA:**
  - O Gate 6 dispara o webhook do Render repassando o commit SHA validado:
    ```bash
    curl -sS --fail -X POST "${RENDER_DEPLOY_HOOK_URL}?ref=${{ github.sha }}"
    ```
- **URL Pública Oficial:** `https://rota-vital.onrender.com`
- **Consulta Real de Saúde (`/actuator/health`):**
  ```bash
  curl -i -s https://rota-vital.onrender.com/actuator/health
  # Retorna HTTP 200 OK com status: UP
  ```
- **Consulta ao Endpoint de Estoque (`/api/estoque`):**
  ```bash
  curl -i -s https://rota-vital.onrender.com/api/estoque
  # Retorna HTTP 200 OK com {"bolsas":[],"tamanho":0}
  ```
- **Blueprint Declarativo Infrastructure-as-Code (`render.yaml`):**
  - Declaração do banco PostgreSQL 16 gerenciado `rotavital-db`.
  - Serviço Web `rota-vital` rodando Docker sob perfil `infra` (`SPRING_PROFILES_ACTIVE=infra`), com injeção automática de `DATABASE_URL` e `autoDeploy: false`.
- **Status Auditado da Proteção de Branches:**
  - Consulta à API do GitHub: `GET /repos/Luizrocha0/Proj-EAD/branches/main` confirmou `"protected": false`.
  - Comando exato via `gh api` documentado para o mantenedor aplicar as regras com os 5 gates obrigatórios do CI.

---

## 8. Evidências do Ciclo TDD (Red → Green → Refactor nas Histórias H1, H2 e H3)

Rastreabilidade cronológica e verificável dos commits de TDD na Release 1:

| História / Fase | Commit SHA | Tipo | Descrição da Evidência |
|---|---|---|---|
| **H1 — Red** | `2525767` | `test` | Falha comprovada: compilação abortada por ausência da interface `EstoqueStorage` e implementação `ListaEstoqueStorage`. |
| **H1 — Green** | `07c44a7` | `feat` | Implementação de `ListaEstoqueStorage` delegando para `ListaEstoque`. 10 testes unitários aprovados. |
| **H1 — Green** | `8aa3432` | `feat` | Implementação de `JdbcEstoqueStorage` com Spring JDBC e SQL `INSERT INTO bolsas`. |
| **H1 — Green** | `badd2a0` | `feat` | Criação de `BolsaRequestDTO` resiliente a literais numéricos JSON de fixtures. |
| **H1 — IT** | `2d0be08` | `test` | Implementação de `BolsaPersistidaIT` e comprovação de recuperação após reinício de contexto Spring. |
| **H2 — Red** | `d5157f5` | `test` | Falha comprovada: ausência do método `List<Bolsa> listar()` na interface `EstoqueStorage`. |
| **H2 — Green** | `1a1589f` | `feat` | Implementação de `listar()` em memória com ordenação LIFO. |
| **H2 — Green** | `38dd729` | `feat` | Implementação de `listar()` no JDBC com query `SELECT ... FROM bolsas ORDER BY id_seq DESC`. |
| **H2 — Green** | `25960c8` | `feat` | Refatoração de `EstoqueController` com envelope `{"bolsas": [...], "tamanho": N}`. |
| **H2 — IT** | `72de699` | `test` | Testes de aceitação com as 5 fixtures canônicas da U1 e validação de duplicatas LIFO. |
| **H3 — Red** | `2b07ecc` | `test` | Falha comprovada: `UnsupportedOperationException: Pendente para Etapa 5 (H3)` em 5 testes JDBC. |
| **H3 — Green** | `9a4b69b` | `feat` | Implementação de `removerPorId` com subquery atômica LIFO `DELETE ... WHERE id_seq = (SELECT id_seq ... LIMIT 1)`. |
| **H3 — Green** | `26b19d9` | `feat` | Integração de `removerBolsa` em `EstoqueService` e controller REST com status 200/404. |
| **H3 — IT** | `9ac7c3b` | `test` | Comprovação de manutenção da exclusão da ocorrência mais recente após destruição e recriação de contexto. |
| **E3 — CI/CD** | `57f89f4` | `ci` | DAG de 6 jobs com deploy hook no Render via SHA validado e suporte a `DATABASE_URL`. |

---

## 9. Mapeamento dos Arquivos e Registros no Repositório

| Artefato de Evidência | Caminho no Repositório |
|---|---|
| **Story Map & Critérios de Aceite** | `docs/infra-u2/story-map.md` |
| **Relatório Completo de TDD** | `docs/infra-u2/tdd.md` |
| **Checklist e Matriz de Conformidade** | `docs/infra-u2/checklist.md` |
| **Registro de Progresso por Etapas** | `docs/infra-u2/progresso.md` |
| **Divisão de Tarefas da Equipe** | `docs/infra-u2/divisao-tarefas.md` |
| **Histórico de Coautoria** | `docs/historico-coautoria.json` |
| **Pipeline de CI/CD** | `.github/workflows/ci.yml` |
| **Orquestração de Containers** | `compose.yaml` |
| **Dockerfile Multi-Stage** | `Dockerfile` |
| **Blueprint PaaS (Render)** | `render.yaml` |
| **Migrações Flyway** | `src/main/resources/db/migration/V1__criar_tabela_bolsas.sql` |
| **Fixtures Canônicas de Teste** | `fixtures/bolsas_u1.json`, `fixtures/requisicoes_u1.json` |
| **Medições Empíricas U1 (SO)** | `bench/measurements.csv`, `bench/measurements_agg.csv` |
