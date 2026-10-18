# Matriz de Conformidade e Checklist de Entrega — U2 (Infraestrutura de Software)

**Projeto:** Rota Vital — Banco de Sangue Inteligente  
**Entrega:** Unidade 2 — Desafios PI — Parte 1  
**Data:** 17/10/2026  
**Autores:** Equipe Rota Vital (Eduardo Borges, Luiz Henrique Rocha, Eliziane Mota, Pedro Iranildo, Ricardo Severiano de Souza Filho)  
**Repositório:** https://github.com/Luizrocha0/Proj-EAD  
**URL Pública:** https://rota-vital.onrender.com  

---

## 1. Visão Geral da Matriz de Conformidade

Este documento apresenta a **Matriz Exaustiva de Conformidade** da entrega da Unidade 2 do projeto **Rota Vital**, confrontando cada requisito formal do enunciado acadêmico e das diretrizes do professor contra:
1. Os arquivos de implementação no repositório;
2. As classes e suítes de testes automatizados associadas;
3. As evidências empíricas e saídas de comandos observadas;
4. O estado de atendimento auditado:
   - `ATENDIDO`: Requisito plenamente implementado, testado com evidência observável e sem pendências.
   - `PENDENTE`: Requisito implementado do ponto de vista de código/configuração, mas dependente de ação administrativa externa ou de infraestrutura de terceiros (com ação exata documentada).
   - `NÃO VERIFICADO`: Item sem verificação executada (evitado nesta entrega através de testes sistemáticos).

---

## 2. Matriz de Conformidade por Bloco de Entrega

### 2.1. Bloco E1: Containerização, Orquestração e Configuração Externa

| ID | Requisito do Enunciado | Arquivos Envolvidos | Testes e Verificação | Evidência Concreta | Estado |
|---|---|---|---|---|:---:|
| **E1.1** | **Orquestração com Docker Compose:** Subir API e banco de dados conjuntamente via comando único padronizado. | `compose.yaml` | `docker compose config --quiet`<br>`docker compose up -d --build` | Sintaxe válida (exit code 0). Ambos os serviços (`rotavital-api` e `rotavital-db`) sobem e alcançam estado `healthy`. | `ATENDIDO` |
| **E1.2** | **PostgreSQL 16 com volume persistente:** Banco relacional configurado em container com armazenamento durável. | `compose.yaml`<br>`Dockerfile` | Inspeção de volume no Compose e reinício de container. | Volume nomeado `rotavital-postgres-data` montado em `/var/lib/postgresql/data`. Dados persistem após `docker compose down && docker compose up -d`. | `ATENDIDO` |
| **E1.3** | **Rede bridge isolada:** Comunicação interna entre API e PostgreSQL sem expor portas desnecessárias ao host. | `compose.yaml` | `docker compose config` | Rede customizada `rotavital-network` com driver `bridge`. API conecta em `jdbc:postgresql://db:5432/rotavital`. | `ATENDIDO` |
| **E1.4** | **Healthcheck do banco e dependência estrita:** API aguarda prontidão do banco antes de iniciar. | `compose.yaml` | Healthcheck com `pg_isready`<br>`depends_on: condition: service_healthy` | Container `db` roda `pg_isready -U rotavital -d rotavital`. A API só inicia após o banco responder conexões TCP. | `ATENDIDO` |
| **E1.5** | **Healthcheck nativo da API:** Verificação contínua da saúde da aplicação e conectividade JDBC. | `compose.yaml`<br>`Dockerfile`<br>`application-infra.properties` | Chamada ao endpoint `/actuator/health` via `wget`. | Resposta HTTP 200 OK com `{"status":"UP","components":{"db":{"status":"UP"}}}` sob perfil `infra`. | `ATENDIDO` |
| **E1.6** | **Dockerfile multi-stage otimizado:** Separação entre ambiente de compilação (JDK) e ambiente de execução (JRE enxuta). | `Dockerfile` | Build da imagem com inspeção de camadas. | Imagem base `eclipse-temurin:21-jre-alpine` gerando artefato final leve, sem ferramentas de build ou código-fonte. | `ATENDIDO` |
| **E1.7** | **Execução de testes no build do Dockerfile:** Eliminação do `-DskipTests` no estágio builder. | `Dockerfile` | Execução de `./mvnw package -DskipITs -q` no Dockerfile. | Qualquer quebra em teste unitário falha imediatamente o build da imagem Docker (comprovado via injeção intencional de falha). | `ATENDIDO` |
| **E1.8** | **Segurança non-root no runtime:** Aplicação executando sob usuário sem privilégios administrativos. | `Dockerfile` | `addgroup -S rotavital && adduser -S rotavital -G rotavital && USER rotavital` | Container roda sob usuário `rotavital` (UID/GID não zero), prevenindo escalonamento de privilégios. | `ATENDIDO` |
| **E1.9** | **Consumo formal de `JAVA_OPTS`:** Repasse de flags de heap e GC ao processo Java. | `Dockerfile`<br>`compose.yaml` | `ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]` | Parâmetros de memória (`-Xms256m -Xmx512m`) repassados com `exec`, tornando a JVM o processo PID 1 para sinais POSIX. | `ATENDIDO` |
| **E1.10** | **Configuração externa e blindagem de segredos:** Parâmetros documentados sem expor credenciais reais. | `.env.example`<br>`.gitignore`<br>`.dockerignore` | Validação de exclusão no git e inspeção de arquivo de exemplo. | `.env.example` documenta todas as variáveis com valores seguros. `.env` e variações estão estritamente no `.gitignore` e `.dockerignore`. | `ATENDIDO` |

---

### 2.2. Bloco E2: TDD, Persistência Relacional e Segregação de Níveis

| ID | Requisito do Enunciado | Arquivos Envolvidos | Testes e Verificação | Evidência Concreta | Estado |
|---|---|---|---|---|:---:|
| **E2.1** | **Preservação estrita da entrega da U1:** Manter códigos C e Java intactos, sem coleções prontas e com estruturas de nós próprios. | `aed-c/`<br>`src/main/java/com/rotavital/aed/u1/`<br>`bench/` | `make -C aed-c test`<br>`mvnw test`<br>`python bench/plot_speedup.py --check` | 54 asserções em C aprovadas com sanitizers; 29 testes legados Java aprovados; 7 testes Python aprovados; CSVs e gráficos intactos. | `ATENDIDO` |
| **E2.2** | **Arquitetura de Portas e Adaptadores (Perfis Spring):** Desacoplamento entre lógica de negócio e armazenamento físico. | `EstoqueStorage.java`<br>`ListaEstoqueStorage.java`<br>`JdbcEstoqueStorage.java`<br>`EstoqueService.java` | `EstoqueServiceTest.java`<br>`EstoqueServiceU1ProfileInjectionTest`<br>`EstoqueServiceInfraProfileInjectionTest` | `EstoqueService` delega 100% para `EstoqueStorage`. Perfil `u1` ativa `ListaEstoqueStorage` (memória) e perfil `infra` ativa `JdbcEstoqueStorage` (PostgreSQL). | `ATENDIDO` |
| **E2.3** | **Proibição de fallback silencioso para memória:** Se o banco falhar sob perfil `infra`, a operação deve falhar explicitamente. | `JdbcEstoqueStorage.java`<br>`EstoqueService.java` | `EstoqueServiceTest.java` (cenário `naoDeveFazerFallbackParaMemoria`) | Exceção `DataAccessException` é propagada imediatamente resultando em HTTP 500/503 e Actuator `DOWN`. Sem mascaramento de erro. | `ATENDIDO` |
| **E2.4** | **História H1 (TDD) — Cadastro e Recuperação Persistente:** Gravação no PostgreSQL e recuperação de todos os atributos. | `JdbcEstoqueStorage.java`<br>`BolsaRequestDTO.java`<br>`EstoqueController.java` | `JdbcEstoqueStorageTest`<br>`BolsaRequestDTOTest`<br>`EstoqueControllerTest`<br>`JdbcEstoqueStorageIT` | Ciclo Red (commit `2525767`) $\to$ Green (commits `07c44a7`, `8aa3432`, `badd2a0`). Cadastro e busca por ID validados. | `ATENDIDO` |
| **E2.5** | **H1 — Sobrevivência pós-reinício:** Dados persistem após encerramento e recriação do contexto Spring. | `BolsaPersistidaIT.java` | `mvnw -Pintegration verify -Dit.test=BolsaPersistidaIT#devePersistirBolsaAposReinicioContexto -Dtest=none` | `applicationContext.close()` executado; novo contexto lê os mesmos dados do PostgreSQL com 100% de paridade. | `ATENDIDO` |
| **E2.6** | **História H2 (TDD) — Listagem LIFO e Envelope Padronizado:** Ordenação mais recente primeiro e envelope JSON. | `JdbcEstoqueStorage.java`<br>`EstoqueController.java` | `JdbcEstoqueStorageTest`<br>`EstoqueControllerTest`<br>`EstoqueControllerIT` | Ciclo Red (commit `d5157f5`) $\to$ Green (commits `1a1589f`, `38dd729`, `25960c8`). Envelope `{"bolsas": [...], "tamanho": N}` com `ORDER BY id_seq DESC`. | `ATENDIDO` |
| **E2.7** | **H2 — Estoque Vazio e IDs Duplicados:** Tratamento correto de ausência de registros e múltiplos registros com mesmo ID. | `JdbcEstoqueStorage.java`<br>`BolsaPersistidaIT.java` | `EstoqueControllerTest`<br>`BolsaPersistidaIT#devePreservarListagemEOrdemLIFOAposReinicioContexto` | Estoque vazio retorna `{"bolsas": [], "tamanho": 0}`. IDs repetidos são exibidos integralmente na listagem geral e busca pontual retorna o mais recente. | `ATENDIDO` |
| **E2.8** | **História H3 (TDD) — Remoção Persistente e Duplicatas:** Exclusão da ocorrência mais recente sem afetar anteriores. | `JdbcEstoqueStorage.java`<br>`EstoqueService.java`<br>`EstoqueController.java` | `JdbcEstoqueStorageTest`<br>`EstoqueServiceTest`<br>`EstoqueControllerTest`<br>`JdbcEstoqueStorageIT` | Ciclo Red (commit `2b07ecc`) $\to$ Green (commits `9a4b69b`, `26b19d9`). Subquery atômica LIFO `WHERE id_seq = (SELECT id_seq ... LIMIT 1)`. Retorna HTTP 404 se inexistente. | `ATENDIDO` |
| **E2.9** | **H3 — Persistência da Remoção pós-reinício:** Exclusão confirmada no banco após destruição de contexto. | `BolsaPersistidaIT.java` | `mvnw -Pintegration verify -Dit.test=BolsaPersistidaIT#deveManterRemocaoAposReinicioContexto -Dtest=none` | A ocorrência excluída não reaparece em novos contextos; a duplicata anterior é preservada intacta. | `ATENDIDO` |
| **E2.10** | **Migrações de Banco com Flyway:** Versionamento do esquema SQL com chave técnica LIFO. | `V1__criar_tabela_bolsas.sql`<br>`pom.xml` | Execução automática de migração no startup sob perfil `infra`. | Tabela `bolsas` criada com `id_seq BIGSERIAL PRIMARY KEY` sem restrição UNIQUE em `id`. Índice composto `idx_bolsas_id_seq ON bolsas(id, id_seq DESC)`. | `ATENDIDO` |
| **E2.11** | **Segregação Surefire vs Failsafe:** Separação estrita no Maven entre testes rápidos e testes de integração. | `pom.xml` | `mvnw test` (Surefire)<br>`mvnw -Pintegration verify` (Failsafe) | Surefire roda `**/*Test.java` (201 testes em ~23s). Failsafe roda `**/*IT.java` (39 métodos com Testcontainers). | `ATENDIDO` |
| **E2.12** | **Testes com PostgreSQL Real (Testcontainers):** Integração sem mocks de banco. | `JdbcEstoqueStorageIT.java`<br>`BolsaPersistidaIT.java`<br>`EstoqueControllerIT.java` | Testcontainers dinâmico no Maven Failsafe. | Contêiner `postgres:16-alpine` provisionado durante os testes de integração, rodando migrações Flyway e DML real. | `ATENDIDO` |

---

### 2.3. Bloco E3: CI/CD, Deploy Automatizado no Render e Governança

| ID | Requisito do Enunciado | Arquivos Envolvidos | Testes e Verificação | Evidência Concreta | Estado |
|---|---|---|---|---|:---:|
| **E3.1** | **Pipeline CI/CD estruturado em DAG:** 6 jobs no GitHub Actions com dependências encadeadas. | `.github/workflows/ci.yml` | Validação de sintaxe e dependências `needs:` do workflow. | DAG: `check-benchmark`, `test-c`, `test-java-unit` $\to$ `test-java-integration` $\to$ `build-docker-smoke` $\to$ `deploy-render`. | `ATENDIDO` |
| **E3.2** | **Eliminação de contornos permissivos no CI:** Zero tolerância a falhas mascaradas na esteira. | `.github/workflows/ci.yml` | Varredura de texto no YAML do pipeline. | Zero `continue-on-error`, zero `\|\| true`. Qualquer falha quebra a esteira e bloqueia o deploy. | `ATENDIDO` |
| **E3.3** | **Smoke Test containerizado completo no CI:** Validação de CRUD REST ponta a ponta em contêineres reais. | `.github/workflows/ci.yml` | Gate 5 (`build-docker-smoke`) executando Compose up e 9 etapas REST. | Valida Actuator com banco, listagem vazia, cadastro (H1), consulta (H1), listagem populada (H2), remoção (H3), 404 e 400. | `ATENDIDO` |
| **E3.4** | **Deploy condicionado e seguro no Render:** Disparo exclusivo na branch `main` repassando o commit SHA aprovado. | `.github/workflows/ci.yml` | `if: github.ref == 'refs/heads/main'`<br>Disparo do hook via `curl -sS` | Hook dispara `curl -sS --fail -X POST "${RENDER_DEPLOY_HOOK_URL}?ref=${{ github.sha }}"`. Publica exclusivamente o commit validado. | `ATENDIDO` |
| **E3.5** | **Proteção de Segredos no CI:** Credenciais de deploy e banco sem vazamento em logs públicos. | `.github/workflows/ci.yml` | Inspeção de saídas e logs mascarados do GitHub Actions. | Secrets `${{ secrets.RENDER_DEPLOY_HOOK_URL }}` injetadas via ambiente; comando `curl -sS` suprime URL nos logs; SHA registrado publicamente. | `ATENDIDO` |
| **E3.6** | **Blueprint Declarativo Infrastructure-as-Code:** Manifest oficial para provisionamento no Render. | `render.yaml` | Validação contra especificações Blueprint do Render. | Declara Web Service `rota-vital` rodando Docker com `autoDeploy: false`, perfil `infra`, e banco PostgreSQL gerenciado `rotavital-db`. | `ATENDIDO` |
| **E3.7** | **Suporte Robusto a `DATABASE_URL` no Spring:** Conversão e blindagem do DataSource para nuvem. | `DatabaseConfig.java`<br>`DatabaseConfigTest.java` | 18 testes unitários rigorosos em `DatabaseConfigTest`. | Converte `postgres://` para `jdbc:postgresql:`, valida portas (1..65535), trata IPv6, remove fragmentos URI, suporta fallbacks e sanitiza caminhos. | `ATENDIDO` |
| **E3.8** | **URL Pública de Produção Ativa:** API respondendo na nuvem sob o domínio documentado. | Infraestrutura Render | `curl -i -s https://rota-vital.onrender.com/actuator/health`<br>`curl -i -s https://rota-vital.onrender.com/api/estoque` | Retorna HTTP 200 OK com status `UP` e `{"bolsas":[],"tamanho":0}`. Instância pública acessível. | `ATENDIDO` |
| **E3.9** | **Procedimento Operacional de Rollback em 3 Vias:** Mecanismos auditáveis para recuperação de desastres. | `README.md`<br>`docs/infra-u2/progresso.md` | Documentação formal das 3 vias (Hook com SHA anterior, Dashboard Render, Git Revert com PR). | Procedimento detalhado passo a passo com comandos e tags de versão identificáveis. | `ATENDIDO` |
| **E3.10** | **Governança de Migrações Flyway (Expand / Contract):** Salvaguardas contra perda de dados e quebra de esquema. | `application-infra.properties`<br>`docs/infra-u2/progresso.md` | `spring.flyway.clean-disabled=true` ativado no perfil `infra`. | Proibição de comandos destrutivos (`flyway:clean`); imutabilidade de scripts `V<N>__*.sql` via checksum; evolução aditiva de colunas. | `ATENDIDO` |
| **E3.11** | **GitHub Flow com Branches de Trabalho e Commits Semânticos:** Organização do repositório por fluxo ágil. | Histórico Git / Branches | `git log --oneline`, branch `etapa/e3-deploy-render`. | Branches dedicadas para cada incremento; commits semânticos rastreáveis (`ci:`, `feat:`, `test:`, `docs:`, `fix:`). | `ATENDIDO` |
| **E3.12** | **Proteção de Branches Remota na Branch `main`:** Exigir aprovação dos 5 gates de CI antes do merge. | Configuração do Repositório GitHub | Inspeção remota via GitHub API: `GET /repos/Luizrocha0/Proj-EAD/branches/main`. | **Estado auditado: `"protected": false`**. Configuração pronta e comando exato via `gh api` documentado para o mantenedor aplicar com seu PAT administrativo. | `PENDENTE` *(Ação Administrativa Externa)* |
| **E3.13** | **Configuração de Secrets no GitHub Repository:** Injeção das credenciais `RENDER_DEPLOY_HOOK_URL` e `RENDER_APP_URL`. | Configuração de Secrets GitHub | Inspeção de execução do workflow no CI. | Configuração de segredo no cofre do GitHub depende de acesso administrativo pelo proprietário do repositório (`Luizrocha0`). | `PENDENTE` *(Ação Administrativa Externa)* |
| **E3.14** | **Aplicação do Blueprint com Banco no Render:** Provisionamento de `rotavital-db` para expor componente `"db"` no Actuator. | Dashboard do Render.com | Chamada a `https://rota-vital.onrender.com/actuator/health`. | Instância pública responde 200 OK mas ainda sem componente `"db"` (PostgreSQL). Pendente do clique em "Apply Blueprint" pelo mantenedor no Render. | `PENDENTE` *(Ação Operacional Externa)* |

---

### 2.4. Bloco E4 / Etapa 8: Documentação Consolidada, Catálogo de Evidências e Apresentação

| ID | Requisito do Enunciado | Arquivos Envolvidos | Testes e Verificação | Evidência Concreta | Estado |
|---|---|---|---|---|:---:|
| **DOC.1** | **Consolidação do README.md na Raiz:** Documento profissional com os 6 tópicos formais (Integrantes, Como rodar, Testes, CI/CD, Decisões e Uso de IA). | `README.md` | Inspeção estrutural e validação de âncoras/links. | Identificação dos 5 membros, instruções Compose/Maven (u1 vs infra), tabelas de comandos e endpoints REST. | `ATENDIDO` |
| **DOC.2** | **Tabela das 12 Decisões Arquiteturais Justificadas:** Mapeamento formal de escolha, alternativas e justificativa concreta para os 12 tópicos obrigatórios. | `README.md` | Inspeção exaustiva dos 12 tópicos técnicos. | Alpine, Multi-stage, Postgres 16, Spring JDBC, Flyway, Volume, Rede bridge, Testcontainers, Surefire/Failsafe, GitHub Flow, GitHub Actions e Render/Secrets. | `ATENDIDO` |
| **DOC.3** | **Declaração Transparente de Uso de IA:** Ferramentas, prompts principais, validação humana, partes revisadas e auditoria adversarial. | `README.md` | Auditoria de conteúdo ético. | Descrição detalhada dos prompts de TDD, engenharia reversa, blindagem de DTO e auditoria independente em 3 rodadas. | `ATENDIDO` |
| **DOC.4** | **Catálogo de Evidências Centralizado:** Catálogo estruturado de todas as evidências de teste, container e deploy. | `docs/evidencias/README.md` | Auditoria de integridade referencial e âncoras GFM. | 9 categorias indexando 201 testes unitários, 39 de integração, 54 em C, 7 em Python, Compose, CI em DAG e commits TDD. | `ATENDIDO` |
| **DOC.5** | **Roteiro Estruturado de Apresentação em Sala:** Planejamento de 15 a 20 min com divisão de falas entre os 5 integrantes e FAQ técnico da banca. | `docs/infra-u2/apresentacao.md` | Validação de distribuição de tempo e tópicos. | Blocos 1 a 5 atribuídos nominalmente; guia rápido de comandos para demonstração ao vivo; FAQ com 8 perguntas críticas (Rollback, DAG, Docker, TDD, Flyway). | `ATENDIDO` |
| **DOC.6** | **Matriz de Conformidade e Checklist de Entrega:** Confrontação item a item de requisitos, implementação, testes e estado auditado. | `docs/infra-u2/checklist.md` | Validação cruzada com enunciado e prompt por etapas. | Matriz com 43 requisitos confrontados com código e saídas empíricas; segregação rigorosa de pendências administrativas externas. | `ATENDIDO` |
| **DOC.7** | **Governança do Registro de Progresso:** Acompanhamento formal de etapas concluídas e resumos padronizados de 5 itens. | `docs/infra-u2/progresso.md` | Auditoria de histórico e protocolo por etapas. | Todas as Etapas 0 a 9 marcadas como Concluídas; parecer formal e matriz de rastreabilidade emitidos conforme Seção 10. | `ATENDIDO` |

---

## 3. Resumo Quantitativo de Conformidade

```
┌─────────────────────────────────────────────────────────────┐
│ MATRIZ DE CONFORMIDADE U2 — STATUS CONSOLIDADO              │
├────────────────────────────────┬──────────────┬─────────────┤
│ Classificação                  │ Quantidade   │ Percentual  │
├────────────────────────────────┼──────────────┼─────────────┤
│ Itens ATENDIDOS (Autônomos)    │ 40           │ 93.0%       │
│ Itens PENDENTES (Ação Externa) │ 3            │ 7.0%        │
│ Itens NÃO VERIFICADOS          │ 0            │ 0.0%        │
├────────────────────────────────┼──────────────┼─────────────┤
│ TOTAL DE REQUISITOS MAPEADOS   │ 43           │ 100.0%      │
└────────────────────────────────┴──────────────┴─────────────┘
```

### Detalhamento das 3 Pendências Externas (Ação Necessária do Mantenedor `Luizrocha0`):
1. **Ativação da Proteção de Branch `main` no GitHub:**
   - *Comando no Windows (PowerShell):*
     ```powershell
     @'
     {
       "required_status_checks": {
         "strict": true,
         "contexts": [
           "SO — Validação das Medições e Speedup (Python)",
           "AED C — GCC + Sanitizers (Address + Undefined)",
           "Java — Testes Unitários (Surefire)",
           "Java — Integração e Aceitação (Failsafe + Testcontainers)",
           "Docker — Build da Imagem e Smoke Test Containerizado"
         ]
       },
       "enforce_admins": true,
       "required_pull_request_reviews": {
         "dismiss_stale_reviews": true,
         "require_code_owner_reviews": false,
         "required_approving_review_count": 1
       },
       "restrictions": null,
       "allow_force_pushes": false,
       "allow_deletions": false
     }
     '@ | gh api --method PUT repos/Luizrocha0/Proj-EAD/branches/main/protection --input -
     ```
   - *Comando no Linux / Bash / macOS:*
     ```bash
     gh api --method PUT repos/Luizrocha0/Proj-EAD/branches/main/protection \
       --input - << 'EOF'
     {
       "required_status_checks": {
         "strict": true,
         "contexts": [
           "SO — Validação das Medições e Speedup (Python)",
           "AED C — GCC + Sanitizers (Address + Undefined)",
           "Java — Testes Unitários (Surefire)",
           "Java — Integração e Aceitação (Failsafe + Testcontainers)",
           "Docker — Build da Imagem e Smoke Test Containerizado"
         ]
       },
       "enforce_admins": true,
       "required_pull_request_reviews": {
         "dismiss_stale_reviews": true,
         "require_code_owner_reviews": false,
         "required_approving_review_count": 1
       },
       "restrictions": null,
       "allow_force_pushes": false,
       "allow_deletions": false
     }
     EOF
     ```
2. **Configuração da Secret no GitHub Actions:**
   - Adicionar `RENDER_DEPLOY_HOOK_URL` nas Secrets de Actions do repositório com o webhook gerado no Web Service do Render.
3. **Aplicação do Blueprint no Render:**
   - No painel `dashboard.render.com`, sincronizar o repositório como Blueprint para criar o banco gerenciado `rotavital-db` declarado em `render.yaml`.
