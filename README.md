# 🩸 Rota Vital — Banco de Sangue Inteligente

> **Backend Spring Boot de Nível Industrial para Gestão de Banco de Sangue**  
> Desenvolvido para a disciplina de **Infraestrutura de Software (Sistemas Operacionais)** e **Algoritmos e Estruturas de Dados (AED)** — Projeto Integrador 2026.2.

[![CI — Rota Vital](https://github.com/Luizrocha0/Proj-EAD/actions/workflows/ci.yml/badge.svg)](https://github.com/Luizrocha0/Proj-EAD/actions/workflows/ci.yml)
[![Status do Deploy](https://img.shields.io/badge/Deploy-Render.com-46E3B7?logo=render&logoColor=white)](https://rota-vital.onrender.com/actuator/health)
[![Java Version](https://img.shields.io/badge/Java-21%20LTS-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.3-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16%20Alpine-336791?logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Docker](https://img.shields.io/badge/Docker-Multi--stage-2496ED?logo=docker&logoColor=white)](https://www.docker.com/)

---

## 👥 Integrantes da Equipe

- **Eduardo Borges**
- **Luiz Henrique Rocha**
- **Eliziane Mota**
- **Pedro Iranildo**
- **Ricardo Severiano de Souza Filho**

- **Repositório:** [https://github.com/Luizrocha0/Proj-EAD](https://github.com/Luizrocha0/Proj-EAD)  
- **URL Pública de Produção:** [https://rota-vital.onrender.com](https://rota-vital.onrender.com)  

---

## 📋 Índice

- [1. Visão Geral e Transição Pedagógica (U1 → U2)](#1-visão-geral-e-transição-pedagógica-u1-u2)
- [2. Como Rodar](#2-como-rodar)
  - [Pré-requisitos](#pré-requisitos)
  - [Configuração de Ambiente (.env.example vs .env)](#configuração-de-ambiente-envexample-vs-env)
  - [Execução via Docker Compose (Recomendado)](#execução-via-docker-compose-recomendado)
  - [Execução Direta com Maven (Perfis u1 vs infra)](#execução-direta-com-maven-perfis-u1-vs-infra)
- [3. Testes por Nível](#3-testes-por-nível)
  - [Pirâmide de Testes e Segregação Maven](#pirâmide-de-testes-e-segregação-maven)
  - [Comandos de Execução](#comandos-de-execução)
  - [Relatórios Gerados e Limitações Observadas](#relatórios-gerados-e-limitações-observadas)
- [4. Pipeline de CI/CD e Deploy Automatizado](#4-pipeline-de-cicd-e-deploy-automatizado)
  - [Arquitetura da Esteira em DAG (6 Jobs)](#arquitetura-da-esteira-em-dag-6-jobs)
  - [Eliminação de Contornos Permissivos](#eliminação-de-contornos-permissivos)
  - [Deploy Hook Seguro no Render com Commit SHA](#deploy-hook-seguro-no-render-com-commit-sha)
  - [Procedimento Operacional de Rollback em 3 Vias](#procedimento-operacional-de-rollback-em-3-vias)
  - [Governança de Migrações com Flyway (Expand / Contract)](#governança-de-migrações-com-flyway-expand-contract)
- [5. Tabela das 12 Decisões Arquiteturais Justificadas](#5-tabela-das-12-decisões-arquiteturais-justificadas)
- [6. Declaração Transparente de Uso de IA](#6-declaração-transparente-de-uso-de-ia)
- [7. Catálogo de Endpoints REST](#7-catálogo-de-endpoints-rest)
- [8. Benchmark de Paralelismo e Medições de SO (U1)](#8-benchmark-de-paralelismo-e-medições-de-so-u1)
- [9. Estrutura Completa do Repositório](#9-estrutura-completa-do-repositório)

---

## 1. Visão Geral e Transição Pedagógica (U1 → U2)

O **Rota Vital** é um sistema para automação e rastreamento de bancos de sangue, projetado para triagem de bolsas, atendimento emergencial de requisições hospitalares e detecção de duplicatas através de paralelismo.

### O Patrimônio Acadêmico da Unidade 1 (AED e SO)
Na **Unidade 1**, a aplicação priorizou fundamentos de ciência da computação:
- Estruturas de dados manuais com alocação dinâmica de nós e ponteiros implementadas em C (`aed-c/`) e replicadas fielmente em Java (`com.rotavital.aed.u1`): lista simplesmente encadeada de bolsas (inserção na cabeça $O(1)$, busca $O(n)$) e fila FIFO de requisições hospitalares.
- Testes exaustivos com sanitizers de memória do GCC (`-fsanitize=address,undefined`) comprovando ausência de memory leaks.
- Paralelismo com threads de plataforma e medições empíricas brutas de speedup para detecção de duplicatas textuais com cálculo de distância de Levenshtein $O(n^2 \cdot L^2)$.

### A Evolução na Unidade 2 (Infraestrutura de Software — Release 1)
Na **Unidade 2**, sem descartar o patrimônio da U1, elevamos a aplicação para um padrão de engenharia de software moderno:
- **Desacoplamento por Portas e Adaptadores (Hexagonal Mínimo):** Criação da interface `EstoqueStorage`. Sob o perfil padrão **`u1`**, a aplicação opera em memória com as estruturas de nós manuais e sem necessidade de banco de dados. Sob o perfil **`infra`**, a aplicação opera com persistência relacional no **PostgreSQL 16**.
- **Proibição Estrita de Fallback Silencioso:** Sob o perfil `infra`, caso o banco de dados fique indisponível, a aplicação lança exceção imediata (`DataAccessException`), falha a requisição HTTP (500/503) e o Actuator reporta `status: DOWN`. Em hipótese alguma há desvio silencioso para a memória.
- **TDD Rigoroso em Três Níveis:** Histórias H1 (Cadastro e Recuperação durável pós-reinício), H2 (Listagem LIFO com envelope padronizado e estoque vazio) e H3 (Remoção persistente da ocorrência mais recente em IDs duplicados) desenvolvidas pelo ciclo *Red $\to$ Green $\to$ Refactor*.
- **Containerização Otimizada:** Dockerfile multi-stage com compilação e testes no build (sem `-DskipTests`), runtime non-root enxuto com Alpine Linux, e orquestração completa via Docker Compose com healthchecks encadeados.
- **Esteira CI/CD em DAG de 6 Gates:** Pipeline no GitHub Actions sem contornos permissivos, smoke test REST containerizado, deploy hook com commit SHA aprovado no Render e rollback auditável em 3 vias com governança Flyway (Expand/Contract).

---

## 2. Como Rodar

### Pré-requisitos

| Ferramenta | Versão Recomendada | Finalidade |
|---|---|---|
| **Java JDK** | 21 LTS (ou 25) | Compilação e execução da aplicação Spring Boot |
| **Maven** | 3.9+ | Gerenciamento de build (Maven Wrapper incluso: `./mvnw` / `.\mvnw.cmd`) |
| **Docker & Compose** | 24+ (Compose v2+) | Orquestração da API e do banco PostgreSQL |
| **Python** | 3.11+ | Validação dos scripts estatísticos e benchmarks da U1 |
| **GCC & Make** | 11+ | Compilação e testes das estruturas C em `aed-c/` |

> 💡 **Nota para Usuários Windows:** No PowerShell, certifique-se de configurar a variável de ambiente `JAVA_HOME` na sessão antes de invocar o Maven Wrapper (`.\mvnw.cmd`), prevenindo o erro `JAVA_HOME not found in your environment`:
> ```powershell
> $env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-25.0.4.101-hotspot"  # Ajuste para o caminho do seu JDK
> ```

---

### Configuração de Ambiente (.env.example vs .env)

A aplicação utiliza variáveis de ambiente para configuração dinâmica. Um modelo seguro e documentado está disponível em `.env.example`:

```bash
# Copiar o arquivo de exemplo para criar o seu .env local
cp .env.example .env

# No Windows PowerShell:
Copy-Item .env.example .env
```

**Principais Variáveis de Ambiente:**

| Variável | Valor Padrão no Compose | Descrição |
|---|---|---|
| `PORT` | `8080` | Porta HTTP exposta pela API no host |
| `POSTGRES_DB` | `rotavital` | Nome do banco de dados relacional |
| `POSTGRES_USER` | `rotavital` | Usuário do banco de dados |
| `POSTGRES_PASSWORD` | `rotavital` | Senha do banco de dados |
| `POSTGRES_PORT` | `5432` | Porta externa do PostgreSQL no host |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://db:5432/rotavital` | URL JDBC de conexão da API com o container do banco |
| `JAVA_OPTS` | `-Xms256m -Xmx512m` | Parâmetros de gerenciamento de heap e memória da JVM |

> 🔒 **Blindagem de Segredos:** O arquivo `.env` está registrado no `.gitignore` e no `.dockerignore`, impedindo o vazamento inadvertido de credenciais reais em commits ou camadas de build do Docker.

---

### Execução via Docker Compose (Recomendado)

O Docker Compose sobe a aplicação completa sob o perfil `infra`, conectando-a automaticamente a um contêiner PostgreSQL 16 com volume persistente.

1. **Subir os serviços em segundo plano com build:**
   ```bash
   docker compose up -d --build
   ```

2. **Verificar a integridade e saúde dos contêineres:**
   ```bash
   docker compose ps
   ```
   *Ambos os contêineres (`rotavital-api` e `rotavital-db`) devem apresentar status `Up (healthy)`.*

3. **Inspecionar os logs em tempo real:**
   ```bash
   docker compose logs -f api
   ```

4. **Verificar o endpoint de saúde do Actuator:**
   ```bash
   curl http://localhost:8080/actuator/health
   ```
   *Resposta esperada (HTTP 200 OK):*
   ```json
   {
     "status": "UP",
     "components": {
       "db": {
         "status": "UP",
         "details": {
           "database": "PostgreSQL",
           "validationQuery": "isValid()"
         }
       },
       "diskSpace": { "status": "UP" },
       "ping": { "status": "UP" }
     }
   }
   ```

5. **Encerramento limpo preservando dados:**
   ```bash
   docker compose down
   ```
   > ⚠️ **Aviso de Integridade:** NUNCA execute `docker compose down -v` a menos que deseje conscientemente destruir o volume nomeado `rotavital-postgres-data` e apagar todos os dados persistidos do estoque.

---

### Execução Direta com Maven (Perfis u1 vs infra)

O sistema suporta execução local sem contêineres através de perfis do Spring Boot:

#### Perfil `u1` (Padrão em Memória — Demonstração Pedagógica da U1)
Inicia a aplicação 100% autônoma em memória. As autoconfigurações de banco, DataSource e migrações Flyway são explicitamente desabilitadas em `application-u1.properties`.

```bash
# Linux / macOS
./mvnw spring-boot:run

# Windows PowerShell
$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-25.0.4.101-hotspot"  # Ajuste para seu JDK
.\mvnw.cmd spring-boot:run
```
*A aplicação subirá utilizando a lista simplesmente encadeada manual com nós `NoBolsa`. Ideal para validações rápidas de sala de aula sem requerer Docker ou banco local instalado.*

#### Perfil `infra` (Persistência no PostgreSQL)
Conecta a aplicação a um banco PostgreSQL rodando no host ou em contêiner (ex: `docker compose up -d db`):

```bash
# Linux / macOS
./mvnw spring-boot:run -Dspring-boot.run.profiles=infra

# Windows PowerShell
$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-25.0.4.101-hotspot"
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=infra
```

---

## 3. Testes por Nível

### Pirâmide de Testes e Segregação Maven

A suíte de testes do Rota Vital adota a arquitetura de pirâmide de testes com fail-fast na esteira, segregada no `pom.xml`:

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│ NÍVEL 3: TESTES DE ACEITAÇÃO HTTP (MockMvc + PostgreSQL Real / Testcontainers)  │
│ • Validação de ponta a ponta dos endpoints REST /api/estoque                     │
│ • Envelopes JSON, status HTTP (200, 400, 404, 500) e cabeçalhos                 │
├─────────────────────────────────────────────────────────────────────────────────┤
│ NÍVEL 2: TESTES DE INTEGRAÇÃO (Spring Data JDBC + PostgreSQL / Testcontainers) │
│ • Execução real de DDL/DML, transações (@Transactional) e Flyway               │
│ • Comprovação de persistência pós-reinício de contexto (BolsaPersistidaIT)      │
├─────────────────────────────────────────────────────────────────────────────────┤
│ NÍVEL 1: TESTES UNITÁRIOS E CARACTERIZAÇÃO (Surefire + Mocks + Memória)         │
│ • Estruturas manuais U1 em C e Java, serialização DTO, sanitização e fail-fast   │
│ • Execução ultrarrápida (~23s) sem dependência de Docker ou banco externo       │
└─────────────────────────────────────────────────────────────────────────────────┘
```

---

### Comandos de Execução

#### 1. Testes Unitários Java Rápidos (Nível 1 — Surefire):
Executa exclusivamente classes `**/*Test.java` sem subir contêineres nem banco de dados.
```bash
./mvnw test
# No Windows PowerShell:
$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-25.0.4.101-hotspot"
.\mvnw.cmd test
```
*Resultado: **201 testes aprovados** (0 falhas, 0 erros, tempo ~23s).*

#### 2. Testes de Integração e Aceitação (Níveis 2 e 3 — Failsafe + Testcontainers):
Executa classes `**/*IT.java` instanciando automaticamente um contêiner PostgreSQL 16 via Testcontainers.
```bash
./mvnw -Pintegration verify
# No Windows PowerShell:
$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-25.0.4.101-hotspot"
.\mvnw.cmd -Pintegration verify
```
*Resultado: **39 métodos de teste aprovados** em 3 classes IT (`JdbcEstoqueStorageIT`, `BolsaPersistidaIT`, `EstoqueControllerIT`).*

#### 3. Testes das Estruturas AED em C (U1 — AddressSanitizer + UBSan):
Compila e executa as 54 asserções em C com sanitizers de memória do GCC.
```bash
# Linux / macOS / WSL
make -C aed-c test

# Windows com MinGW
mingw32-make -C aed-c CFLAGS="-Wall -Wextra -std=c11" LDFLAGS="" test
```
*Resultado: **54/54 asserções aprovadas** (23 em `test_lista` e 31 em `test_fila`), **zero memory leaks**.*

#### 4. Testes dos Benchmarks e Paralelismo em Python (U1 — SO):
```bash
# Testes unitários dos cálculos estatísticos
python -m unittest discover -s bench -p 'test_*.py'

# Validação estatística de consistência das 40 medições brutas
python bench/plot_speedup.py --check
```
*Resultado: **7/7 testes aprovados** e `Agregado correto: 8 configurações`.*

---

### Relatórios Gerados e Limitações Observadas

- **Relatórios de Testes:**
  - Relatórios Surefire: `target/surefire-reports/`
  - Relatórios Failsafe: `target/failsafe-reports/`
- **Limitações Técnicas Observadas:**
  - **Sanitizers no Windows:** A distribuição padrão MinGW do Windows não empacota a biblioteca `libasan`. Para compilar com `-fsanitize=address,undefined`, utilize Linux nativo, contêiner Docker ou WSL.
  - **Script de Benchmark (`bench/measure.ps1`):** Não deve ser executado em rotinas normais de teste ou auditoria, pois realiza coletas ativas contra `localhost:8080` que regravam os CSVs históricos de 40 medições da U1.
  - **Plot sem `--check` (`bench/plot_speedup.py`):** Sem a flag `--check`, o script regrava `bench/measurements_agg.csv` convertendo terminações de linha para CRLF no Windows, sujando o controle de versão. Use sempre com `--check`.

---

## 4. Pipeline de CI/CD e Deploy Automatizado

### Arquitetura da Esteira em DAG (6 Jobs)

O workflow em `.github/workflows/ci.yml` estrutura a validação em um Grafo Acíclico Dirigido (DAG) com dependências explícitas:

```
                    ┌──────────────────────┐
                    │ check-benchmark (SO) │
                    └──────────┬───────────┘
                               │
 ┌──────────────┐   ┌──────────▼───────────┐
 │ test-c (AED) │   │ test-java-unit (L1)  │
 └──────┬───────┘   └──────────┬───────────┘
        │                      │
        │           ┌──────────▼───────────┐
        │           │test-java-integr. (L2)│
        │           └──────────┬───────────┘
        │                      │
        └──────────────┬───────┴───────────────┐
                       ▼
          ┌───────────────────────────┐
          │ build-docker-smoke (Gate 5) │
          └────────────┬──────────────┘
                       │ (exclusivo branch main)
                       ▼
          ┌───────────────────────────┐
          │   deploy-render (Gate 6)  │
          │  ref=${{ github.sha }}    │
          └───────────────────────────┘
```

1. **Gate 1 (`check-benchmark`):** Executa testes Python e checa consistência das medições da U1.
2. **Gate 2 (`test-c`):** Compila e executa estruturas C com GCC, AddressSanitizer e UndefinedBehaviorSanitizer.
3. **Gate 3 (`test-java-unit`):** Roda os 201 testes unitários Java rápidos via Surefire.
4. **Gate 4 (`test-java-integration`):** Executa os testes de integração com banco PostgreSQL via Testcontainers.
5. **Gate 5 (`build-docker-smoke`):** Constrói a imagem Docker (testando no build), sobe o Compose localmente, aguarda saúde de API e banco, e executa smoke test funcional de ponta a ponta com 9 asserções REST completas (cadastro, consulta, listagem LIFO, remoção, 404 e 400).
6. **Gate 6 (`deploy-render`):** Dispara o deploy hook no Render com o commit SHA aprovado.

---

### Eliminação de Contornos Permissivos

A esteira de CI foi construída com disciplina estrita:
- **Zero `continue-on-error`:** Nenhuma etapa possui permissão para falhar silenciosamente.
- **Zero `|| true`:** Nenhum comando mascara códigos de saída de erro.
- Qualquer falha em qualquer gate bloqueia imediatamente a progressão e impede a publicação em produção.

---

### Deploy Hook Seguro no Render com Commit SHA

Para evitar a publicação de versões defasadas ou intermediárias decorrentes de builds assíncronos no Git, o job de deploy aciona o webhook do Render repassando explicitamente o identificador do commit aprovado:

```bash
curl -sS --fail -X POST "${RENDER_DEPLOY_HOOK_URL}?ref=${{ github.sha }}"
```

- **Proteção de Segredos:** A URL do hook é injetada via secret do repositório (`${{ secrets.RENDER_DEPLOY_HOOK_URL }}`) e o `curl` roda com `-sS`, impedindo vazamento de tokens nos logs de execução do GitHub Actions.
- **Blueprint Oficial:** O manifesto `render.yaml` declara o Web Service `rota-vital` rodando Docker sob perfil `infra` e provisiona o banco de dados gerenciado PostgreSQL 16 `rotavital-db`.

---

### Procedimento Operacional de Rollback em 3 Vias

Em caso de degradação em produção pós-release, a equipe conta com três vias auditáveis de reversão:

1. **Via 1 — Rollback Imediato via Deploy Hook com SHA Estável:**
   ```bash
   curl -sS --fail -X POST "${RENDER_DEPLOY_HOOK_URL}?ref=<COMMIT_SHA_ESTAVEL>"
   ```
   *O Render interrompe o container atual e reconstrói a imagem a partir do commit estável indicado.*

2. **Via 2 — Rollback Manual pelo Painel Web do Render:**
   - Acesse `https://dashboard.render.com` $\to$ Selecione o Web Service `rota-vital`.
   - Navegue até a aba **"Deploys"**.
   - Localize a última versão estável com tag "Live" e clique em **"Rollback to this deploy"**.

3. **Via 3 — Rollback via Git Revert com PR:**
   - Crie uma branch de correção: `git checkout -b fix/revert-release`.
   - Reverta o merge comprometido: `git revert -m 1 <MERGE_COMMIT_SHA>`.
   - Abra um Pull Request para a `main`. Todos os 5 gates de CI reexecutarão as validações antes do merge e novo deploy.

---

### Governança de Migrações com Flyway (Expand / Contract)

Para garantir que operações de rollback não causem perda ou corrupção de dados:
- **Estratégia Expand / Contract:**
  - *Fase Expand:* Novas colunas ou tabelas são introduzidas como opcionais (`NULLABLE`) ou com valores `DEFAULT`. Nenhuma coluna existente é removida ou renomeada na mesma release.
  - *Fase Migrate:* Se um rollback ocorrer, a versão antiga de código continua funcionando normalmente, pois os campos dos quais depende permanecem intactos.
  - *Fase Contract:* Apenas após a estabilização confirmada da nova versão, uma migração futura descarta colunas obsoletas.
- **Imutabilidade de Scripts:** Arquivos `V<N>__*.sql` aplicados nunca são alterados. O Flyway calcula o checksum SHA-256 e impede execução com esquemas adulterados.
- **Salvaguarda `clean-disabled`:** Em `application-infra.properties`, configuramos `spring.flyway.clean-disabled=true`, desabilitando permanentemente qualquer comando destrutivo do Flyway em produção.

---

## 5. Tabela das 12 Decisões Arquiteturais Justificadas

A tabela abaixo registra formalmente as 12 decisões técnicas fundamentais tomadas durante o projeto, comparando a escolha adotada contra suas alternativas reais e explicitando a justificativa concreta:

| Nº | Tópico Arquitetural | Escolha Adotada | Alternativas Consideradas | Justificativa Concreta |
|:---:|---|---|---|---|
| **1** | **Imagem Base Docker** | `eclipse-temurin:21-jre-alpine` | Debian Slim, Ubuntu, RedHat UBI | Imagem com menos de 180MB, superfície de ataque mínima (sem compiladores, package managers ou utilitários desnecessários no runtime) e compatibilidade total com Java 21 LTS. |
| **2** | **Multi-stage Build** | 2 Estágios (Builder JDK 21 + Runtime JRE 21 Alpine) | Build monostage, compilação no host copiando JAR pronto | Isola o código-fonte e dependências Maven do contêiner final, garante compilação reprodutível independente do host e executa testes unitários na criação da imagem sem poluir o runtime. |
| **3** | **Banco de Dados Relacional** | **PostgreSQL 16** (`postgres:16-alpine`) | MySQL, H2, SQLite, MongoDB | Banco relacional maduro, suporte a transações ACID estritas, tipagem de dados rica, sequências monotônicas (`BIGSERIAL`) para resolução de concorrência LIFO e ampla integração em provedores PaaS (Render). |
| **4** | **Acesso a Dados: Spring JDBC vs JPA** | **Spring Data JDBC (`JdbcTemplate`)** | Spring Data JPA / Hibernate, MyBatis | Domínio cirúrgico e focado da Release 1. O JDBC proporciona controle atômico total sobre o SQL gerado (essencial para a subquery LIFO `ORDER BY id_seq DESC LIMIT 1`), menor overhead de memória, startup instantâneo e ausência de problemas comuns de ORM (dirty checking complexo, lazy loading e cache de 1º nível). |
| **5** | **Versionamento de Banco** | **Flyway Core** | Liquibase, scripts manuais `.sql` | Migrações SQL nativas com versionamento linear determinístico (`V1__...sql`), controle de checksum SHA-256 e integração transparente com o ciclo de vida do Spring Boot sem demandar DSLs XML/YAML proprietárias. |
| **6** | **Persistência de Containers** | **Volume Nomeado Docker** (`rotavital-postgres-data`) | Bind mounts no host, contêiner efêmero | Preserva dados entre reinicializações de contêineres e reboots do host, isola a gestão de permissões de arquivo das particularidades do SO hospedeiro e segue as melhores práticas de portabilidade Docker. |
| **7** | **Isolamento de Rede** | **Rede Bridge Isolada** (`rotavital-network`) | Rede padrão `bridge`, rede `host` | Impede a exposição desnecessária de portas do PostgreSQL à rede pública/host, permitindo comunicação interna estrita entre serviços através de DNS interno do Docker (`db:5432`). |
| **8** | **Testes de Integração com Banco** | **Testcontainers com PostgreSQL 16 Real** | Mocks de JDBC, H2 em memória | Elimina falsos-positivos decorrentes de diferenças de dialeto SQL (funções de data, subqueries, `BIGSERIAL`). O teste executa contra o exato mesmo motor e versão de banco que roda em produção. |
| **9** | **Segregação de Testes Maven** | **Surefire (`*Test.java`) vs Failsafe (`*IT.java`)** | Surefire para tudo, suíte única | Viabiliza fail-fast imediato: testes unitários rodam no build do Dockerfile e no CI em ~23s sem depender de Docker. Testes de integração pesados com contêiner rodam na fase `verify` via perfil `-Pintegration`. |
| **10** | **Estratégia de Branches / PR** | **GitHub Flow com Branches de Entrega e Commits Semânticos** | GitFlow tradicional, Trunk-Based com push direto | Permite isolamento seguro de incrementos funcionais (H1, H2, H3), facilidade de auditoria via Pull Requests rastreáveis e proteção da branch principal contra quebras. |
| **11** | **Ferramenta de CI/CD** | **GitHub Actions** | GitLab CI, Jenkins, CircleCI | Integração nativa com o repositório GitHub, runners gerenciados com Docker e Linux pré-instalados, suporte a matrizes de jobs em DAG e publicação de artefatos de teste de forma simples. |
| **12** | **Provedor PaaS e Segredos** | **Render.com com Secrets no GitHub Actions** | AWS ECS, Heroku, Railway | Provedor gratuito aderente ao escopo do projeto, suporte nativo a blueprints declarativos (`render.yaml`), deploy hooks via webhook autenticado e banco PostgreSQL gerenciado. |

---

## 6. Declaração Transparente de Uso de IA

Em estrito cumprimento às diretrizes acadêmicas do professor e aos princípios éticos de transparência e integridade de software:

### Ferramentas Utilizadas
- **Modelos e Agentes:** Google Gemini (família 1.5/2.0), Antigravity Engine e Agentes SWE Light para automação e assistência no ciclo de engenharia de software.

### Principais Casos de Uso e Prompts de Engenharia
1. **Engenharia Reversa e Mapeamento de Contratos da U1:** Identificação das estruturas manuais de lista encadeada e fila FIFO, mapeamento dos contratos REST de `EstoqueController` e análise de complexidade temporal e espacial.
2. **Arquitetura Hexagonal Mínima:** Geração de boilerplate para desacoplamento via interface `EstoqueStorage` e configuração condicional de perfis Spring (`@Profile("u1")` vs `@Profile("infra")`).
3. **Ciclo TDD nas Histórias H1, H2 e H3:** Elaboração de cenários de teste adversariais no ciclo Red (provocando falhas intencionais com asserções rigorosas) antes da geração do código de produção Green.
4. **Resiliência e Blindagem de Tipos:** Identificação de potenciais falhas de deserialização Jackson decorrentes de números de ponto flutuante literais (`450.0`) nas fixtures da U1, auxiliando na modelagem do `BolsaRequestDTO`.
5. **Automação de CI/CD:** Estruturação da DAG de 6 jobs no GitHub Actions e elaboração de scripts resilientes com cURL para deploy hook passando o commit SHA.

### Validação Humana e Auditoria Adversarial
- **Revisão Linha a Linha:** Todas as classes, DTOs, métodos SQL, arquivos YAML e scripts foram minuciosamente inspecionados, revisados e criticados pelos membros da equipe humana antes de qualquer merge.
- **Execução Empírica Real:** Nenhuma evidência de teste ou medição foi fabricada. Todos os 201 testes Java, 54 asserções em C, 7 testes Python e comandos Docker foram executados em terminais reais com logs auditados.
- **Auditoria Adversarial Independente:** Cada etapa da U2 foi submetida a rodadas de auditoria adversarial, que identificaram e corrigiram falhas como: ausência de consumo formal de `JAVA_OPTS` no entrypoint, quebras de linha CRLF em scripts de shell, race condition na captura de status HTTP no deploy hook e necessidade de isolamento estrito de `@Transactional` contra falhas sob o perfil `u1`.

---

## 7. Catálogo de Endpoints REST

A API expõe serviços sob a porta padrão `8080`:

### Estoque de Bolsas de Sangue (`/api/estoque`)

| Método | Endpoint | Descrição | Status de Sucesso | Perfil Ativo |
|---|---|---|:---:|:---:|
| `POST` | `/api/estoque` | Cadastra uma nova bolsa no estoque | `200 OK` | `u1` e `infra` |
| `GET` | `/api/estoque` | Lista todas as bolsas em ordem LIFO com envelope | `200 OK` | `u1` e `infra` |
| `GET` | `/api/estoque/{id}` | Consulta os dados da bolsa mais recente pelo ID | `200 OK` (ou `404`) | `u1` e `infra` |
| `DELETE` | `/api/estoque/{id}` | Remove a ocorrência mais recente da bolsa | `200 OK` (ou `404`) | `u1` e `infra` |

#### Exemplo de Cadastro (`POST /api/estoque`):
```bash
curl -X POST http://localhost:8080/api/estoque \
  -H "Content-Type: application/json" \
  -d '{
    "id": "BOLSA-001",
    "tipoSanguineo": "O+",
    "volumeMl": 450.0,
    "dataColeta": "2026-09-01",
    "validade": "2026-11-01",
    "doador": "João Silva"
  }'
```
*Resposta:* `{"mensagem": "Bolsa adicionada ao estoque", "tamanho": 1}`

#### Exemplo de Listagem Geral (`GET /api/estoque`):
```bash
curl http://localhost:8080/api/estoque
```
*Resposta:*
```json
{
  "bolsas": [
    {
      "id": "BOLSA-001",
      "tipoSanguineo": "O+",
      "volumeMl": 450.0,
      "dataColeta": "2026-09-01",
      "validade": "2026-11-01",
      "doador": "João Silva"
    }
  ],
  "tamanho": 1
}
```

---

### Fila FIFO de Requisições Hospitalares (`/api/requisicoes` — AED U1)

| Método | Endpoint | Descrição | Status de Sucesso |
|---|---|---|:---:|
| `POST` | `/api/requisicoes` | Enfileira requisição emergencial | `200 OK` |
| `POST` | `/api/requisicoes/atender` | Desenfileira e atende a próxima requisição (FIFO) | `200 OK` |
| `GET` | `/api/requisicoes/proxima` | Consulta a próxima requisição sem remover (peek) | `200 OK` |
| `GET` | `/api/requisicoes` | Lista todas as requisições pendentes na fila | `200 OK` |

---

### Health Check e Monitoramento (`/actuator`)

| Método | Endpoint | Descrição |
|---|---|---|
| `GET` | `/actuator/health` | Status de integridade da aplicação e conectividade com PostgreSQL |
| `GET` | `/actuator/info` | Metadados gerais do projeto |

---

## 8. Benchmark de Paralelismo e Medições de SO (U1)

A detecção de duplicatas compara pares de requisições hospitalares utilizando a distância de Levenshtein. O custo algorítmico é de $O(n^2 \cdot L^2)$ para $n$ registros limitados pelo comprimento $L$, permitindo a distribuição paralela das partições entre threads.

A série canônica publicada em **06/10/2026** utiliza exclusivamente as 40 medições empíricas brutas registradas em [bench/measurements.csv](bench/measurements.csv): 8.000 e 16.000 registros, modo `platform`, 1, 2, 4 e 8 threads, com 5 repetições por configuração.

| Tamanho ($n$) | Threads | Tempo Médio HTTP (ms) | Speedup Observado |
|:---:|:---:|:---:|:---:|
| **8000** | 1 | 23.883,00 | 1,00x |
| **8000** | 2 | 18.925,60 | 1,26x |
| **8000** | 4 | 11.105,20 | 2,15x |
| **8000** | 8 | 7.209,80 | 3,31x |
| **16000** | 1 | 97.867,00 | 1,00x |
| **16000** | 2 | 81.400,00 | 1,20x |
| **16000** | 4 | 69.518,20 | 1,41x |
| **16000** | 8 | 41.297,40 | 2,37x |

Consulte a documentação completa da U1 em [docs/medicoes.md](docs/medicoes.md), [docs/justificativa.md](docs/justificativa.md), [docs/analise.md](docs/analise.md) e o relatório técnico formal [docs/RotaVital_Threads_Relatorio.pdf](docs/RotaVital_Threads_Relatorio.pdf).

---

## 9. Estrutura Completa do Repositório

```
rota-vital/
├── .github/workflows/
│   └── ci.yml                        # Esteira CI/CD em DAG com 6 gates sem contornos
├── aed-c/                            # Módulo C pedagógico da U1
│   ├── include/                      # Headers: lista.h, fila.h, bolsa.h, requisicao.h
│   ├── src/u1/                       # Implementações manuais em C com malloc/free
│   ├── tests/                        # 54 asserções em test_lista.c e test_fila.c
│   └── Makefile                      # Build com GCC + AddressSanitizer + UBSan
├── bench/                            # Medições empíricas e benchmarks Python de SO U1
│   ├── measurements.csv              # 40 medições empíricas brutas canônicas
│   ├── measurements_agg.csv          # Agregação estatística oficial
│   ├── plot_speedup.py               # Script de validação estatística e gráficos
│   └── test_plot_speedup.py          # 7 testes unitários do benchmark
├── docs/                             # Documentação técnica e acadêmica consolidada
│   ├── evidencias/
│   │   └── README.md                 # Catálogo detalhado de todas as evidências da U2
│   ├── infra-u2/
│   │   ├── apresentacao.md           # Roteiro estruturado para apresentação em sala
│   │   ├── checklist.md              # Matriz completa de conformidade E1, E2 e E3
│   │   ├── divisao-tarefas.md        # Atribuição formal de responsabilidades
│   │   ├── progresso.md              # Registro exaustivo de etapas e decisões
│   │   ├── story-map.md              # Story map, backbone e matriz rastreável
│   │   └── tdd.md                    # Relatório aprofundado do ciclo Red-Green-Refactor
│   ├── equivalencia-u1.md            # Tradução comentada C <-> Java da U1
│   ├── medicoes.md, analise.md       # Documentos de SO da U1
│   └── speedup.png                   # Gráfico oficial de speedup
├── fixtures/                         # Fixtures sintéticas canônicas (bolsas e requisições)
├── src/main/java/com/rotavital/
│   ├── aed/u1/                       # Estruturas manuais com nós dinâmicos em Java
│   ├── config/                       # DatabaseConfig para conversão e blindagem de URLs
│   ├── controller/                   # EstoqueController, RequisicaoController, DuplicateController
│   ├── domain/                       # Entidades Bolsa e Requisicao
│   ├── dto/                          # BolsaRequestDTO resiliente a números e strings
│   ├── service/                      # Camada de negócio desacoplada via EstoqueStorage
│   └── storage/                      # EstoqueStorage, ListaEstoqueStorage (u1), JdbcEstoqueStorage (infra)
├── src/main/resources/
│   ├── application.properties        # Perfil default u1
│   ├── application-u1.properties     # Isolamento de banco para modo em memória puro
│   ├── application-infra.properties  # Configuração PostgreSQL, Flyway e salvaguardas
│   └── db/migration/
│       └── V1__criar_tabela_bolsas.sql # Esquema com BIGSERIAL LIFO e índices
├── src/test/java/com/rotavital/      # 201 testes unitários e 3 suítes de integração IT
│   ├── aed/u1/, config/, controller/ # Testes unitários Nível 1 (Surefire)
│   ├── dto/, service/, storage/      # Testes unitários e mocks de persistência
│   └── integration/                  # Suítes Nível 2 e 3 (Failsafe + Testcontainers)
│       ├── AbstractPostgresIT.java   # Contêiner singleton PostgreSQL 16
│       ├── BolsaPersistidaIT.java    # Sobrevivência de dados pós-reinício de contexto
│       ├── EstoqueControllerIT.java  # Testes de aceitação REST de ponta a ponta
│       └── JdbcEstoqueStorageIT.java # Testes de persistência direta JDBC e subqueries
├── .dockerignore                     # Proteção contra envio de segredos ao build
├── .env.example                      # Modelo documentado de variáveis de ambiente
├── .gitignore                        # Blindagem estrita de credenciais (.env)
├── compose.yaml                      # Orquestração da API e PostgreSQL com healthchecks
├── Dockerfile                        # Multi-stage com testes no build e usuário non-root
├── pom.xml                           # Configuração Maven segregando Surefire e Failsafe
├── README.md                         # Este documento consolidado
└── render.yaml                       # Blueprint declarativo para deploy no Render
```

---

## 📄 Licença

Projeto acadêmico desenvolvido exclusivamente para fins de avaliação no Projeto Integrador 2026.2.
