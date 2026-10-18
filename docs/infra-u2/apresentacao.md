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

## 1. Visão Geral da Apresentação e Cronograma de Falas

| Bloco | Tópico Principal | Apresentador | Tempo Estimado |
|:---:|---|---|:---:|
| **1** | **Introdução, Contexto e Transição U1 $\to$ U2**<br>• Visão geral do Rota Vital<br>• Patrimônio acadêmico da U1 preservado<br>• Arquitetura Hexagonal Mínima e estratégia de perfis (`u1` vs `infra`) | **Eduardo Borges** | 3 a 4 min |
| **2** | **E1: Containerização e Infraestrutura Docker**<br>• Dockerfile multi-stage otimizado (testes no build sem `-DskipTests`)<br>• Orquestração com Compose (serviços `api` e `db`, rede bridge, volume)<br>• Healthchecks encadeados e segurança non-root | **Luiz Henrique Rocha** | 3 a 4 min |
| **3** | **E2: Persistência Relacional e Metodologia TDD**<br>• O ciclo Red $\to$ Green $\to$ Refactor nas histórias H1, H2 e H3<br>• Desafio técnico: IDs duplicados e ordenação temporal LIFO<br>• DDL Flyway, subqueries atômicas e resiliência de tipos (DTOs) | **Eliziane Mota** | 3 a 4 min |
| **4** | **E2: Pirâmide de Testes em Três Níveis e Não-Regressão**<br>• Segregação Maven (Surefire unitário vs Failsafe integração)<br>• PostgreSQL real com Testcontainers e comprovação pós-reinício (`BolsaPersistidaIT`)<br>• Blindagem da U1 (54 asserções C com sanitizers e benchmarks Python) | **Pedro Iranildo** | 3 a 4 min |
| **5** | **E3: CI/CD no GitHub Actions, Deploy no Render e Demo**<br>• Esteira DAG com 6 jobs sem contornos permissivos<br>• Deploy hook com commit SHA aprovado no Render<br>• Procedimento auditável de Rollback em 3 vias e governança Flyway<br>• Demonstração ao vivo dos endpoints e conclusão | **Ricardo Severiano** | 4 a 5 min |

---

## 2. Roteiro Detalhado por Apresentador (Fala a Fala)

---

### Bloco 1: Introdução, Contexto e Transição U1 $\to$ U2
**Apresentador:** Eduardo Borges  
**Tempo:** ~3 a 4 minutos  
**Slide/Material de Apoio:** Diagrama da Transição Pedagógica e Arquitetura de Portas e Adaptadores.

#### Pontos Principais a Expor:
1. **Apresentação da Equipe e Propósito do Sistema:**
   > *"Boa noite, professor e colegas. Nosso grupo é composto por Eduardo, Luiz Henrique, Eliziane, Pedro e Ricardo. Estamos apresentando a entrega da Unidade 2 de Infraestrutura de Software do projeto Rota Vital. O Rota Vital é um sistema de gestão de banco de sangue concebido para lidar com a triagem, controle durável de estoque de bolsas de sangue, atendimento emergencial de requisições hospitalares e detecção de solicitações duplicadas."*

2. **O Desafio da Transição U1 $\to$ U2:**
   > *"Na Unidade 1, nosso foco foi acadêmico e algorítmico: implementamos estruturas de dados manuais com nós dinâmicos em C e em Java — lista simplesmente encadeada para estoque e fila FIFO para requisições —, comprovando equivalência matemática e paralelismo com threads para cálculo de similaridade Levenshtein.  
   Na Unidade 2, recebemos a missão de elevar esse backend a um padrão industrial de infraestrutura de software: containerizar a solução, persistir os dados em banco relacional real, adotar TDD rigoroso e automatizar o ciclo completo de CI/CD e deploy."*

3. **A Solução Arquitetural — Perfis Spring (`u1` vs `infra`):**
   > *"O desafio central era: como persistir no PostgreSQL sem descartar ou deturpar o código da U1? A solução adotada foi o padrão de Portas e Adaptadores (Arquitetura Hexagonal Mínima). Criamos a interface `EstoqueStorage` desacoplando a regra de negócio do meio físico:
   - Sob o perfil padrão `u1`, ativamos o adaptador `ListaEstoqueStorage`, que opera 100% em memória usando a lista encadeada manual da U1. Excluímos as autoconfigurações de banco, permitindo que a aplicação suba instantaneamente e sem dependências externas.
   - Sob o perfil `infra`, ativamos o `JdbcEstoqueStorage`, que se conecta ao PostgreSQL 16 via Spring JDBC e migrações Flyway.
   Crucial: no perfil `infra`, há proibição estrita de fallback silencioso. Se o banco falhar, o sistema falha imediatamente e reporta status DOWN no Actuator, garantindo fidelidade operacional total."*

---

### Bloco 2: E1 — Containerização e Infraestrutura Docker
**Apresentador:** Luiz Henrique Rocha  
**Tempo:** ~3 a 4 minutos  
**Slide/Material de Apoio:** `Dockerfile`, `compose.yaml` e fluxo de inicialização com healthchecks.

#### Pontos Principais a Expor:
1. **Dockerfile Multi-Stage e Eliminação de Más Práticas:**
   > *"Passando para a parte de containers (E1), reformulamos integralmente a imagem Docker da aplicação. No legado da U1, existia a flag `-DskipTests`, que ignorava testes durante a compilação. Nós eliminamos isso: no estágio de builder, o Dockerfile executa `./mvnw package -DskipITs -q`. Isso significa que qualquer teste unitário que quebre interrompe o build da imagem imediatamente na origem. Usamos `-DskipITs` para não tentar rodar testes com banco no builder, evitando a necessidade insegura de Docker-in-Docker."*

2. **Segurança Non-Root e Controle de Processos:**
   > *"No estágio de runtime, usamos a imagem enxuta `eclipse-temurin:21-jre-alpine`. Criamos um usuário e grupo dedicados sem privilégios (`rotavital`) e definimos `USER rotavital`. Nunca executamos a aplicação como `root`. Além disso, o entrypoint consome formalmente a variável de ambiente `JAVA_OPTS` usando `exec java $JAVA_OPTS -jar app.jar`. O comando `exec` faz com que a JVM assuma o PID 1 no contêiner, respondendo corretamente a sinais POSIX como `SIGTERM`, permitindo o graceful shutdown."*

3. **Orquestração com Docker Compose (`compose.yaml`):**
   > *"Para orquestrar o ambiente local e de homologação, criamos o `compose.yaml` com dois serviços interligados:
   - O serviço `db`, rodando `postgres:16-alpine`, com healthcheck nativo executando `pg_isready` a cada 5 segundos.
   - O serviço `api`, construído a partir do nosso Dockerfile sob o perfil `infra`.
   Configuramos `depends_on: db: condition: service_healthy`. A API não sobe 'às cegas'; ela aguarda ativamente a confirmação de que o PostgreSQL está pronto para aceitar conexões TCP.
   Os dados são protegidos através do volume nomeado `rotavital-postgres-data`, e a comunicação ocorre dentro da rede bridge isolada `rotavital-network`. Também fornecemos o arquivo `.env.example` documentando todas as variáveis, com regras estritas no `.gitignore` e `.dockerignore` para blindar qualquer segredo real."*

---

### Bloco 3: E2 — Persistência Relacional e Metodologia TDD
**Apresentadora:** Eliziane Mota  
**Tempo:** ~3 a 4 minutos  
**Slide/Material de Apoio:** Ciclo TDD (tabela de commits Red/Green), DDL Flyway e Subquery LIFO.

#### Pontos Principais a Expor:
1. **A Disciplina do Ciclo Red $\to$ Green $\to$ Refactor:**
   > *"No bloco E2, a persistência do estoque foi construída seguindo rigorosamente a disciplina do TDD. Não escrevemos uma única linha de código de produção sem antes termos um teste vermelho falhando pelo motivo correto. Todas as etapas estão registradas com commits auditáveis no Git:
   - Para a História H1 (Cadastro e Recuperação): primeiro provocamos a falha Red com classes e DTO ausentes (commit `2525767`), para depois implementar `ListaEstoqueStorage`, `JdbcEstoqueStorage` e o `BolsaRequestDTO` (commits `07c44a7` e `8aa3432`).
   - Para a História H2 (Listagem Persistente e Envelope): primeiro o teste falhou pela ausência do método `listar` (commit `d5157f5`), e então implementamos a ordenação LIFO e o envelope JSON (commits `1a1589f` e `38dd729`).
   - Para a História H3 (Remoção Persistente): primeiro registramos 5 testes lançando `UnsupportedOperationException` (commit `2b07ecc`), para depois implementar a deleção atômica no banco (commit `9a4b69b`)."*

2. **O Desafio dos Identificadores Repetidos e a Ordem LIFO:**
   > *"Um dos requisitos mais interessantes foi preservar a semântica da U1: a lista encadeada permitia cadastrar bolsas com o mesmo ID e sempre consultava ou removia a ocorrência mais recente (comportamento de pilha / LIFO na cabeça).
   Como reproduzir isso no PostgreSQL sem quebrar a integridade relacional?
   No Flyway (`V1__criar_tabela_bolsas.sql`), modelamos a tabela `bolsas` com uma chave técnica autoincrementável: `id_seq BIGSERIAL PRIMARY KEY`. O campo de negócio `id` NÃO possui restrição `UNIQUE`. Isso nos dá duas grandes vantagens:
   1. A ordenação é estritamente dada por `ORDER BY id_seq DESC`, que é monotonicamente crescente e imune a colisões de timestamp no mesmo milissegundo.
   2. Para a remoção em H3, executamos uma subquery atômica em SQL:  
      `DELETE FROM bolsas WHERE id_seq = (SELECT id_seq FROM bolsas WHERE id = ? ORDER BY id_seq DESC LIMIT 1)`.  
      Essa operação apaga cirurgicamente a ocorrência mais recente, deixando as ocorrências mais antigas do mesmo ID intactas no banco, com custo $O(1)$ graças ao índice composto `idx_bolsas_id_seq`."*

3. **Resiliência de Tipagem da API REST:**
   > *"Também blindamos a API contra falhas de deserialização Jackson: criamos o `BolsaRequestDTO`, que aceita o campo `volumeMl` tanto como literal numérico (`450.0`) quanto como texto (`"450.0"`), garantindo interoperabilidade total com as fixtures oficiais da U1."*

---

### Bloco 4: E2 — Pirâmide de Testes em Três Níveis e Não-Regressão
**Apresentador:** Pedro Iranildo  
**Tempo:** ~3 a 4 minutos  
**Slide/Material de Apoio:** Pirâmide de Testes, Relatórios Maven e Comprovação Pós-Reinício.

#### Pontos Principais a Expor:
1. **A Pirâmide de Testes e a Segregação no Maven:**
   > *"Para garantir qualidade sem sacrificar a velocidade de desenvolvimento, organizamos nossos testes em três níveis bem definidos e segregados no `pom.xml`:
   - **Nível 1 (Testes Unitários Rápidos via Surefire):** Executam via `mvnw test`. São 201 testes executados em cerca de 20 segundos! Cobrem regras de domínio, DTOs, limites de caracteres e adaptadores em memória, sem depender de banco ou Docker.
   - **Nível 2 (Testes de Integração via Failsafe + Testcontainers):** Executam via `mvnw -Pintegration verify`. Disparam contêineres reais de PostgreSQL 16 gerenciados pelo Testcontainers. Testam migrações Flyway reais, transações com rollback e persistência.
   - **Nível 3 (Testes de Aceitação HTTP):** Validam os endpoints REST de ponta a ponta via MockMvc com PostgreSQL real, testando envelopes JSON, códigos de status 200, 400 e 404."*

2. **Comprovação de Sobrevivência Pós-Reinício (`BolsaPersistidaIT`):**
   > *"O ponto alto da validação de persistência é a nossa suíte `BolsaPersistidaIT`. Não fizemos apenas testes superficiais. O teste cadastra a bolsa, encerra formalmente o contexto da aplicação com `applicationContext.close()`, instancia um segundo contexto Spring conectado ao mesmo PostgreSQL e comprova que os dados sobrevivem 100% íntegros.  
   Fizemos o mesmo para H2 (atestando a preservação da ordem LIFO após reinício) e para H3 (comprovando que, após reiniciar, a ocorrência removida não volta e a ocorrência anterior permanece lá)."*

3. **Preservação e Blindagem do Legado da U1:**
   > *"Cumprimos rigorosamente a diretriz de integridade: nenhum teste da U1 foi alterado ou enfraquecido. Mantemos 100% de aprovação nas 54 asserções em C compiladas com AddressSanitizer e UndefinedBehaviorSanitizer (zero memory leaks), 7 testes unitários de benchmark em Python e consistência estatística perfeita nas 40 medições históricas de speedup."*

---

### Bloco 5: E3 — Pipeline CI/CD, Deploy no Render, Rollback e Demonstração
**Apresentador:** Ricardo Severiano de Souza Filho  
**Tempo:** ~4 a 5 minutos  
**Slide/Material de Apoio:** DAG do GitHub Actions, Terminais com cURL e URL Pública.

#### Pontos Principais a Expor:
1. **Esteira CI/CD em DAG com 6 Gates (GitHub Actions):**
   > *"No bloco E3, estruturamos nosso pipeline em `.github/workflows/ci.yml` como um Grafo Acíclico Dirigido (DAG) composto por 6 gates encadeados:
   - Gate 1: Validação dos benchmarks Python e speedup (SO).
   - Gate 2: Compilação e testes C com AddressSanitizer (AED).
   - Gate 3: Testes unitários Java via Surefire (Nível 1).
   - Gate 4: Testes de integração e persistência via Failsafe + Testcontainers (Nível 2 e 3).
   - Gate 5: Build da imagem Docker e Smoke Test containerizado completo com Compose, testando todo o ciclo REST (cadastro, consulta, listagem, remoção, 404 e 400).
   - Gate 6: Deploy automatizado no Render.
   Regra de ferro da equipe: zero contornos permissivos. Não há `continue-on-error` nem `|| true`. Qualquer falha quebra a esteira imediatamente."*

2. **Deploy Automatizado no Render com SHA Validado:**
   > *"O Gate 6 só executa se todos os 5 gates anteriores passarem e se a branch for a `main`. Para garantir que o Render não faça build de versões intermediárias ou desatualizadas, utilizamos o deploy hook com parâmetro explícito de ref:  
   `curl -sS --fail -X POST "${RENDER_DEPLOY_HOOK_URL}?ref=${{ github.sha }}"`.  
   Dessa forma, o Render publica exatamente o commit que foi compilado, testado e aprovado pela nossa esteira."*

3. **Procedimento Operacional de Rollback e Governança Flyway:**
   > *"Desenvolvemos um procedimento auditável de rollback em 3 vias:
   - Via 1: Disparo do webhook passando o commit SHA estável anterior.
   - Via 2: Rollback manual via aba 'Deploys' do dashboard do Render.
   - Via 3: Rollback via `git revert` abrindo novo PR validado pelos gates de CI.
   E para garantir que o banco não seja corrompido em caso de reversão de versão, adotamos a estratégia Expand / Contract com Flyway: toda evolução de esquema é aditiva (colunas opcionais ou com defaults), migrações aplicadas são imutáveis e ativamos `spring.flyway.clean-disabled=true` em produção, impedindo qualquer comando destrutivo acidental."*

4. **Demonstração Prática ao Vivo e Conclusão:**
   > *"Para concluir, vamos demonstrar ao vivo a operação da nossa API pública em `https://rota-vital.onrender.com`:
   - Consultamos o Actuator de saúde: `/actuator/health` (HTTP 200 UP).
   - Consultamos o estoque inicial: `/api/estoque` (HTTP 200 com envelope padronizado).
   - Cadastramos uma bolsa de sangue via POST.
   - Consultamos a bolsa pelo ID e listamos o estoque comprovando a ordenação LIFO.
   - Efetuamos a remoção via DELETE e atestamos a baixa do estoque.
   Com isso, entregamos uma infraestrutura robusta, testada, automatizada e que honra plenamente a jornada acadêmica das Unidades 1 e 2. Estamos abertos para perguntas da banca e do professor. Obrigado!"*

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

**P4: Por que a proteção da branch `main` consta como pendente no checklist?**  
*R (Ricardo Severiano / Pedro Iranildo):* Por rigor técnico e honestidade acadêmica. Consultamos a API pública do GitHub (`GET /repos/Luizrocha0/Proj-EAD/branches/main`) e constatamos que a branch está configurada como `"protected": false`. Nós não podemos inventar que ela está protegida. A configuração de proteção de branch exige permissão de administrador no repositório. Documentamos o comando exato via `gh api` no `checklist.md` e no `README.md` para que o proprietário (`Luizrocha0`) aplique a regra com seu Personal Access Token.

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
