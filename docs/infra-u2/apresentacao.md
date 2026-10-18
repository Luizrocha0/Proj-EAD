# Roteiro de Apresentação em Sala de Aula — Rota Vital (Unidade 2)

**Projeto:** Rota Vital — Banco de Sangue Inteligente  
**Entrega:** Unidade 2 — Desafios PI — Parte 1 (Infraestrutura de Software)  
**Semestre:** 2026.2  
**Duração Total Estimada:** 15 a 20 minutos  
**Equipe e Apresentadores:**
- **Eduardo Borges**
- **Luiz Henrique Rocha**
- **Eliziane Mota**
- **Pedro Iranildo**
- **Ricardo Severiano de Souza Filho**

---

## 1. Visão Geral da Apresentação e Cronograma de Tópicos

| Bloco | Tópico Principal | Apresentador Responsável | Tempo Estimado |
|:---:|---|---|:---:|
| **1** | **Introdução, Contexto e Transição U1 $\to$ U2**<br>• Visão geral do projeto Rota Vital<br>• Patrimônio acadêmico da U1 preservado<br>• Arquitetura Hexagonal Mínima e estratégia de perfis (`u1` vs `infra`) | **Eduardo Borges** | 3 a 4 min |
| **2** | **E1: Containerização e Infraestrutura Docker**<br>• Dockerfile multi-stage otimizado (testes unitários no build)<br>• Orquestração com Compose (serviços `api` e `db`, rede bridge, volume persistente)<br>• Healthchecks encadeados e segurança non-root | **Luiz Henrique Rocha** | 3 a 4 min |
| **3** | **E2: Persistência Relacional e Metodologia TDD**<br>• O ciclo Red $\to$ Green $\to$ Refactor nas histórias H1, H2 e H3<br>• Desafio técnico: IDs repetidos e ordenação temporal LIFO<br>• DDL Flyway, subqueries atômicas e resiliência de tipos (DTOs) | **Eliziane Mota** | 3 a 4 min |
| **4** | **E2: Pirâmide de Testes em Três Níveis e Não-Regressão**<br>• Segregação Maven (Surefire unitário vs Failsafe integração)<br>• PostgreSQL real com Testcontainers e comprovação pós-reinício (`BolsaPersistidaIT`)<br>• Blindagem da U1 (54 asserções C com sanitizers e benchmarks Python) | **Pedro Iranildo** | 3 a 4 min |
| **5** | **E3: CI/CD no GitHub Actions, Deploy no Render e Demo**<br>• Esteira DAG com 6 jobs sem contornos permissivos<br>• Deploy hook com commit SHA aprovado no Render<br>• Procedimento auditável de Rollback em 3 vias e governança Flyway<br>• Demonstração ao vivo dos endpoints e conclusão | **Ricardo Severiano** | 4 a 5 min |

---

## 2. Estrutura Detalhada por Slide e Tópicos Técnicos

---

### Bloco 1: Introdução, Contexto e Transição U1 $\to$ U2
**Apresentador:** Eduardo Borges  
**Tempo:** ~3 a 4 minutos  
**Material de Apoio:** Diagrama da Transição Pedagógica e Arquitetura de Portas e Adaptadores.

#### Conteúdo e Tópicos Técnicos do Slide:
- **Abertura e Escopo do Sistema:**
  - Apresentação do projeto Rota Vital (gestão inteligente de banco de sangue).
  - Casos de uso centrais: triagem, controle de estoque persistente de bolsas, atendimento emergencial hospitalar e idempotência de solicitações.
- **Transição Metodológica U1 $\to$ U2:**
  - Da algoritmia manual em memória (C/Java com nós dinâmicos) para infraestrutura de software em padrão de produção.
  - Metas da U2: containerização com Docker, persistência relacional com PostgreSQL, TDD rigoroso e automação completa de CI/CD e deploy.
- **Arquitetura Hexagonal Mínima e Perfis Spring (`u1` vs `infra`):**
  - Desacoplamento da regra de negócio via interface `EstoqueStorage`.
  - **Perfil `u1`:** Ativa `ListaEstoqueStorage` (opera 100% em memória sobre a lista encadeada da U1, sem autoconfiguração de banco e sem dependências externas).
  - **Perfil `infra`:** Ativa `JdbcEstoqueStorage` (conecta ao PostgreSQL 16 via Spring JDBC e migrações Flyway).
  - **Proibição de Fallback Silencioso:** Se o PostgreSQL falhar sob o perfil `infra`, o sistema falha imediatamente (HTTP 503 / Actuator DOWN), eliminando qualquer desvio oculto para memória.

---

### Bloco 2: E1 — Containerização e Infraestrutura Docker
**Apresentador:** Luiz Henrique Rocha  
**Tempo:** ~3 a 4 minutos  
**Material de Apoio:** `Dockerfile`, `compose.yaml` e fluxo de inicialização com healthchecks.

#### Conteúdo e Tópicos Técnicos do Slide:
- **Dockerfile Multi-Stage Otimizado:**
  - Eliminação de más práticas: remoção de `-DskipTests` na compilação.
  - Estágio de build: executa `./mvnw package -DskipITs -q` (qualquer teste quebrado interrompe a geração da imagem na origem).
  - Estágio de runtime enxuto baseado em `eclipse-temurin:21-jre-alpine` (sem ferramentas desnecessárias no container final).
- **Segurança Non-Root e Gerenciamento de Processos:**
  - Execução sob usuário e grupo dedicados sem privilégios (`rotavital` / `USER rotavital`).
  - Consumo obrigatório de `JAVA_OPTS` e invocação com `exec java $JAVA_OPTS -jar app.jar` (JVM assume PID 1 e atende a sinais POSIX `SIGTERM` para encerramento gracioso).
- **Orquestração com Docker Compose (`compose.yaml`):**
  - Serviço `db` (`postgres:16-alpine`) com healthcheck nativo via `pg_isready` (intervalo de 5s).
  - Serviço `api` dependente do banco via `depends_on: db: condition: service_healthy` (inicialização segura e determinística).
  - Isolamento de rede bridge (`rotavital-network`) e persistência garantida via volume nomeado (`rotavital-postgres-data`).
  - Configurações segregadas via `.env.example`, protegidas por regras de `.gitignore` e `.dockerignore`.

---

### Bloco 3: E2 — Persistência Relacional e Metodologia TDD
**Apresentadora:** Eliziane Mota  
**Tempo:** ~3 a 4 minutos  
**Material de Apoio:** Ciclo TDD (tabela de commits Red/Green), DDL Flyway e Subquery LIFO.

#### Conteúdo e Tópicos Técnicos do Slide:
- **Disciplina do Ciclo Red $\to$ Green $\to$ Refactor:**
  - Desenvolvimento orientado estritamente por testes prévios para as histórias H1, H2 e H3.
  - **H1 (Cadastro e Recuperação):** Red com classes ausentes (commit `2525767`) $\to$ Green com `EstoqueStorage` e DTO (commits `07c44a7` e `8aa3432`).
  - **H2 (Listagem Persistente e Envelope):** Red com método `listar` ausente (commit `d5157f5`) $\to$ Green com ordenação LIFO e envelope JSON (commits `1a1589f` e `38dd729`).
  - **H3 (Remoção Persistente):** Red com 5 cenários lançando `UnsupportedOperationException` (commit `2b07ecc`) $\to$ Green com deleção atômica no banco (commit `9a4b69b`).
- **Resolução de Chaves Técnicas e Semântica LIFO:**
  - Preservação da semântica da U1: permissão de bolsas com o mesmo ID de negócio, operando como pilha (LIFO).
  - Modelagem no Flyway (`V1__criar_tabela_bolsas.sql`) com chave técnica `id_seq BIGSERIAL PRIMARY KEY` e campo `id` sem restrição de unicidade.
  - Ordenação determinística com `ORDER BY id_seq DESC` (imune a colisões de timestamp).
  - Remoção atômica via subquery SQL: `DELETE FROM bolsas WHERE id_seq = (SELECT id_seq FROM bolsas WHERE id = ? ORDER BY id_seq DESC LIMIT 1)`.
  - Índice composto `idx_bolsas_id_seq` garantindo custo $O(1)$ na busca e deleção da ocorrência mais recente.
- **Resiliência de Tipagem na API REST:**
  - `BolsaRequestDTO` com deserialização flexível para `volumeMl` (aceita numérico e string), garantindo compatibilidade com fixtures legadas da U1.

---

### Bloco 4: E2 — Pirâmide de Testes em Três Níveis e Não-Regressão
**Apresentador:** Pedro Iranildo  
**Tempo:** ~3 a 4 minutos  
**Material de Apoio:** Pirâmide de Testes, Relatórios Maven e Comprovação Pós-Reinício.

#### Conteúdo e Tópicos Técnicos do Slide:
- **Pirâmide de Testes e Segregação no Maven:**
  - **Nível 1 (Unitários Surefire):** `mvnw test` — 201 testes rápidos (~20s) cobrindo regras de negócio, limites e adaptadores em memória.
  - **Nível 2 (Integração Failsafe + Testcontainers):** `mvnw -Pintegration verify` — PostgreSQL 16 real em contêiner efêmero, validando migrações Flyway e transações JDBC.
  - **Nível 3 (Aceitação HTTP MockMvc):** Validação de ponta a ponta dos endpoints REST com banco real, testando payloads, envelopes e status HTTP (200, 400, 404).
- **Comprovação de Sobrevivência Pós-Reinício (`BolsaPersistidaIT`):**
  - Teste de durabilidade real: persistência de dados, encerramento forçado do contexto (`applicationContext.close()`) e validação de recuperação íntegra em novo contexto.
  - Comprovação pós-reinício para H1 (recuperação), H2 (ordem LIFO) e H3 (exclusão definitiva da ocorrência mais recente sem restaurar registros apagados).
- **Preservação e Blindagem do Legado da U1:**
  - 100% de integridade no código C (54 asserções aprovadas com AddressSanitizer e UndefinedBehaviorSanitizer, zero memory leaks).
  - 7 testes unitários Python e consistência estatística perfeita nas 40 medições históricas de speedup.

---

### Bloco 5: E3 — Pipeline CI/CD, Deploy no Render, Rollback e Demonstração
**Apresentador:** Ricardo Severiano de Souza Filho  
**Tempo:** ~4 a 5 minutos  
**Material de Apoio:** DAG do GitHub Actions, Terminais com cURL e URL Pública.

#### Conteúdo e Tópicos Técnicos do Slide:
- **Esteira CI/CD em DAG com 6 Gates (GitHub Actions):**
  - Estrutura em Grafo Acíclico Dirigido (`.github/workflows/ci.yml`):
    - Gate 1: Benchmarks Python e speedup (SO).
    - Gate 2: Compilação e testes C com Sanitizers (AED).
    - Gate 3: Testes unitários Java Surefire (Nível 1).
    - Gate 4: Testes de integração Failsafe + Testcontainers (Níveis 2 e 3).
    - Gate 5: Build da imagem Docker e Smoke Test REST com Compose (9 passos).
    - Gate 6: Deploy automatizado no Render.
  - Política de tolerância zero: ausência de contornos (`continue-on-error` ou `|| true`).
- **Deploy Automatizado no Render com SHA Validado:**
  - Disparo de deploy hook restrito à branch `main` e condicionado à aprovação dos 5 gates prévios.
  - Injeção obrigatória do parâmetro `?ref=${{ github.sha }}` garantindo a publicação exata do commit auditado.
- **Procedimento Operacional de Rollback e Governança Flyway:**
  - Procedimento em 3 vias: disparo de webhook com SHA anterior estável, reversão manual no painel do Render ou `git revert` com PR auditado.
  - Governança de banco Expand/Contract: migrações Flyway puramente aditivas, imutáveis e proteção de produção com `spring.flyway.clean-disabled=true`.
- **Demonstração Prática ao Vivo e Conclusão:**
  - Consulta ao Actuator de saúde: `/actuator/health` (HTTP 200 UP com componente `db`).
  - Demonstração do ciclo REST completo: listagem inicial, cadastro via POST, consulta por ID, comprovação da ordenação LIFO e exclusão via DELETE.

---

## 3. Guia Rápido de Comandos para Demonstração ao Vivo

Caso o professor solicite a execução ao vivo de comandos no terminal durante a apresentação:

### 1. Testes Unitários Java Rápidos (Surefire):
```powershell
$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-25.0.4.101-hotspot"
.\mvnw.cmd test
# Exibe 201 testes aprovados em ~23s
```

### 2. Testes de Integração com Banco Real (Failsafe + Testcontainers):
```powershell
.\mvnw.cmd -Pintegration verify
# Sobe Testcontainers com PostgreSQL 16 e executa as 3 classes IT
```

### 3. Testes em C com Sanitizers (AED U1):
```bash
mingw32-make -C aed-c CFLAGS="-Wall -Wextra -std=c11" LDFLAGS="" test
# Exibe 54 asserções em C aprovadas sem memory leaks
```

### 4. Validação dos Benchmarks Python (SO U1):
```powershell
python -m unittest discover -s bench -p 'test_*.py'
python bench/plot_speedup.py --check
```

### 5. Subida Local via Docker Compose:
```bash
docker compose up -d --build
docker compose ps
# Exibe ambos os serviços rotavital-api e rotavital-db saudáveis
```

### 6. Demonstração de Perfis Maven (u1 vs infra):
```powershell
# Modo u1: Em memória pura, sem necessidade de banco de dados (ideal para aula rápida)
$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-25.0.4.101-hotspot"
.\mvnw.cmd spring-boot:run

# Modo infra: Persistência relacional no PostgreSQL 16
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=infra
```

### 7. Comprovação da Proibição de Fallback Silencioso:
```bash
# Caso o PostgreSQL seja interrompido sob o perfil infra, a API falha imediatamente:
docker compose stop db
curl -i http://localhost:8080/actuator/health
# Exibe status DOWN com componente db DOWN (sem desvio para memória)
docker compose start db
```

### 8. Chamadas cURL contra a API Pública (`https://rota-vital.onrender.com`):
```bash
# Health Check
curl -i https://rota-vital.onrender.com/actuator/health

# Listar Estoque
curl -i https://rota-vital.onrender.com/api/estoque

# Cadastrar Bolsa
curl -i -X POST https://rota-vital.onrender.com/api/estoque \
  -H "Content-Type: application/json" \
  -d '{"id":"BOLSA-DEMO","tipoSanguineo":"O+","volumeMl":450.0,"dataColeta":"2026-10-17","validade":"2026-12-17","doador":"Apresentação U2"}'

# Consultar por ID
curl -i https://rota-vital.onrender.com/api/estoque/BOLSA-DEMO

# Remover Bolsa
curl -i -X DELETE https://rota-vital.onrender.com/api/estoque/BOLSA-DEMO
```

---

## 4. FAQ — Preparação para Perguntas Prováveis da Banca

**P1: Por que não utilizaram Spring Data JPA / Hibernate em vez de Spring Data JDBC?**  
*R (Eduardo Borges / Eliziane Mota):* Optamos conscientemente por Spring Data JDBC (`JdbcTemplate`) porque o domínio da Release 1 é delimitado e cirúrgico (apenas a entidade `Bolsa`). O JDBC nos dá controle total e determinístico sobre o SQL executado, permitindo escrever exatamente a subquery atômica com `ORDER BY id_seq DESC LIMIT 1` para exclusão LIFO de duplicatas sem overhead de cache de primeiro nível, dirty checking, proxy Hibernate ou risco de `LazyInitializationException`. Além disso, o JDBC inicializa em milissegundos e consome menos memória no container Alpine.

**P2: Como vocês garantem que a remoção de duplicatas apaga a ocorrência certa em concorrência?**  
*R (Eliziane Mota / Eduardo Borges):* A remoção é atômica no nível do banco via subquery SQL: `DELETE FROM bolsas WHERE id_seq = (SELECT id_seq FROM bolsas WHERE id = ? ORDER BY id_seq DESC LIMIT 1)`. Como o PostgreSQL executa a subquery dentro da mesma transação com lock de linha na escrita, duas requisições concorrentes para deletar o mesmo ID apagam sequencialmente a mais recente e a penúltima, sem leituras sujas nem retorno espúrio de erro.

**P3: Como o Dockerfile garante que nenhum teste quebrado vá para produção?**  
*R (Luiz Henrique Rocha):* No estágio builder, removemos deliberadamente a flag `-DskipTests` e executamos `./mvnw package -DskipITs -q`. Se qualquer teste unitário falhar, o Maven retorna exit code 1 e o Docker aborta o build da imagem imediatamente, gerando falha visível no Gate 5 do GitHub Actions. Além disso, o runtime roda como usuário non-root `rotavital` e o entrypoint consome `exec java $JAVA_OPTS -jar app.jar` para assumir o PID 1 e responder adequadamente a sinais POSIX `SIGTERM`.

**P4: Como vocês configuraram e comprovaram a proteção da branch `main` no GitHub?**  
*R (Ricardo Severiano / Pedro Iranildo):* A proteção da branch `main` está formalmente ativada e verificada via GitHub REST API (`"protected": true`). Configuramos regras estritas de proteção exigindo aprovação obrigatória dos 5 status checks do CI (`SO`, `AED C`, `Java Unit`, `Java Integration` e `Docker Smoke`) antes de qualquer merge. Com isso, ninguém consegue fazer push direto na `main` nem mesclar PRs com testes quebrados, garantindo a integridade contínua do branch de produção.

**P5: Como a equipe comprova que o desenvolvimento das histórias H1, H2 e H3 foi genuinamente guiado por TDD (Red $\to$ Green $\to$ Refactor)?**  
*R (Eliziane Mota / Pedro Iranildo):* Através de rastreabilidade de commits atômicos no Git:
- **História H1:** Iniciamos com falha provocada pela ausência das classes `EstoqueStorage` e `BolsaRequestDTO` (commit `2525767`), para depois implementar o código de produção em memória e no JDBC (commits `07c44a7` e `8aa3432`) e o teste de persistência durável pós-reinício de contexto `BolsaPersistidaIT` (commit `2d0be08`).
- **História H2:** Provocamos falha intencional de compilação pela falta do método `listar` (commit `d5157f5`), implementamos o retorno ordenado LIFO e o envelope JSON (commits `1a1589f`, `38dd729` e `25960c8`), e validamos com as fixtures oficiais da U1.
- **História H3:** Criamos 5 cenários com falha explícita lançando `UnsupportedOperationException` (commit `2b07ecc`), e só então codificamos a subquery atômica de remoção LIFO no PostgreSQL (commits `9a4b69b` e `26b19d9`), comprovando com `BolsaPersistidaIT` que a exclusão persiste após o encerramento da JVM.
Além disso, escrevemos suítes de caracterização para blindar os comportamentos da U1 antes de qualquer refatoração.

**P6: Por que a esteira de CI/CD foi estruturada em DAG com 6 gates encadeados em vez de uma sequência monolítica?**  
*R (Ricardo Severiano / Luiz Henrique Rocha):* A esteira em DAG (`.github/workflows/ci.yml`) aplica os princípios de Fail-Fast e isolamento de responsabilidades:
1. Gates independentes e rápidos (Gate 1 Python, Gate 2 C11 com sanitizers e Gate 3 Java unitário Surefire) rodam em paralelo no início.
2. Somente se os três passarem, o Gate 4 (Failsafe + Testcontainers com PostgreSQL real) é iniciado, economizando tempo de execução.
3. Se os testes com banco passarem, o Gate 5 constrói a imagem Docker (testando no build) e sobe o Docker Compose local para executar um Smoke Test REST de 9 etapas contra o ambiente containerizado.
4. O Gate 6 (Deploy no Render) só executa se todos os 5 gates anteriores passarem e se a branch for a `main`.
Eliminamos 100% de contornos permissivos (zero `continue-on-error`, zero `|| true`), garantindo que nenhuma versão quebrada chegue à produção.

**P7: Como funciona o procedimento de Rollback em 3 vias e como a governança Flyway evita quebras no banco de dados?**  
*R (Ricardo Severiano / Eduardo Borges):* Disponibilizamos 3 vias auditáveis de reversão para recuperação de desastres:
- **Via 1 (Deploy Hook Imediato):** Disparo de cURL passando a referência do commit estável anterior (`?ref=<COMMIT_SHA>`).
- **Via 2 (Painel do Render):** Reversão manual com um clique na aba 'Deploys' do dashboard para a versão estável prévia.
- **Via 3 (Git Revert com PR):** Reversão de merge commit via `git revert -m 1`, abrindo PR que reexecuta todos os gates do CI antes do merge.
Para que o banco não quebre em rollbacks, adotamos a governança **Expand / Contract** com Flyway: toda evolução de esquema é puramente aditiva (colunas opcionais ou com defaults); migrações aplicadas são estritamente imutáveis (checksum validado pelo Flyway); e ativamos `spring.flyway.clean-disabled=true` no perfil `infra`, impedindo comandos destrutivos acidentais em produção.

**P8: Quais foram as principais diretrizes de segurança e orquestração adotadas no Docker Compose (E1)?**  
*R (Luiz Henrique Rocha / Pedro Iranildo):* No `compose.yaml`, implementamos orquestração orientada a resiliência:
1. Rede bridge customizada e isolada (`rotavital-network`), permitindo que a API e o PostgreSQL conversem via DNS interno sem expor portas de banco para o host.
2. Volume nomeado persistente (`rotavital-postgres-data`), preservando os dados do estoque entre reinicializações do contêiner.
3. Healthcheck nativo no PostgreSQL usando `pg_isready` e dependência estrita na API com `depends_on: db: condition: service_healthy`, impedindo que a aplicação suba antes da prontidão do banco.
4. Segregação estrita de variáveis através de `.env.example`, protegendo o `.env` real no `.gitignore` e `.dockerignore`.
