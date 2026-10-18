# Relatório de TDD e Matriz Rastreável de Testes — Rota Vital (Infra U2)

**Projeto:** Rota Vital — Banco de Sangue Inteligente (Unidade 2 — Desafios PI — Parte 1)  
**Documento:** Relatório Exaustivo de TDD, Segregação de Níveis e Matriz Red → Green  
**Data:** 17/10/2026  
**Status:** Auditado e Concluído (Etapa 9 — Auditoria Final da U2)  
**Autores:** Equipe Rota Vital (Eduardo Borges, Luiz Henrique Rocha, Eliziane Mota, Pedro Iranildo, Ricardo Severiano de Souza Filho)

---

## 1. Metodologia Red → Green → Refactor na Release 1 (U2)

O desenvolvimento da persistência relacional na Unidade 2 do projeto **Rota Vital** seguiu com estrita fidelidade o ciclo **TDD (Test-Driven Development)** clássico:

```
    ┌────────────────────────────────────────────────────────┐
    │                                                        │
    ▼                                                        │
┌──────────────┐     Código Mínimo      ┌──────────────┐    Refatoração   ┌───────────────┐
│     RED      │ ─────────────────────► │    GREEN     │ ───────────────► │   REFACTOR    │
│ (Teste falha │                        │ (Passa no    │                  │ (Limpeza sem  │
│  por falta   │                        │  comporta-   │                  │  regressão)   │
│  de recurso) │                        │  mento alvo) │                  │               │
└──────────────┘                        └──────────────┘                  └───────────────┘
```

### 1.1. Princípios Operacionais Inegociáveis
1. **Nenhum código de produção antes do teste vermelho:** Nenhum método, adaptador JDBC ou rota HTTP foi implementado sem a prévia existência de um teste executável que falhasse pelo motivo correto (comportamento ausente, e nunca por erros de infraestrutura ou sintaxe).
2. **Proibição de testes enfraquecidos ou fabricados:** Nenhum teste preexistente foi enfraquecido, ignorado (`@Disabled`), comentado ou contornado. Se um teste falhou, o código de produção foi corrigido para satisfazer a especificação.
3. **Isolamento e Independência de Estados:** Os testes de persistência realizam limpeza determinística (`@BeforeEach`) e utilizam contextos Spring isolados (`@DirtiesContext`), impedindo poluição cruzada entre cenários.
4. **Commits semânticos e auditáveis no Git:** A sequência cronológica dos commits no repositório retrata fielmente as etapas Red (`test:`), Green (`feat:`) e Refatoração/Auditoria (`fix:`, `docs:`).

---

## 2. Segregação e Hierarquia de Testes por Nível

A arquitetura de testes do Rota Vital foi estruturada em três níveis bem delimitados, garantindo fail-fast na esteira e custo computacional proporcional:

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│ NÍVEL 3: TESTES DE ACEITAÇÃO HTTP (MockMvc + PostgreSQL Real / Testcontainers)  │
│ • Validação de ponta a ponta dos endpoints REST /api/estoque                     │
│ • Envelopes JSON, status HTTP (200, 400, 404, 500) e cabeçalhos                 │
└──────────────────────────────────────┬──────────────────────────────────────────┘
                                       │
┌──────────────────────────────────────▼──────────────────────────────────────────┐
│ NÍVEL 2: TESTES DE INTEGRAÇÃO (Spring Data JDBC + PostgreSQL / Testcontainers) │
│ • Execução real de DDL/DML, transações (@Transactional) e Flyway               │
│ • Comprovação de persistência pós-reinício de contexto (BolsaPersistidaIT)      │
└──────────────────────────────────────┬──────────────────────────────────────────┘
                                       │
┌──────────────────────────────────────▼──────────────────────────────────────────┐
│ NÍVEL 1: TESTES UNITÁRIOS E CARACTERIZAÇÃO (JUnit 5 + Mocks + Memória)          │
│ • Estruturas manuais U1 em C e Java, serialização DTO, sanitização e fail-fast   │
│ • Execução em milissegundos sem qualquer dependência externa ou banco           │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### Comandos Padronizados no Maven
- **Testes Unitários Rápidos (Nível 1):**
  ```powershell
  .\mvnw.cmd test
  ```
  *(Surefire executa exclusivamente `**/*Test.java` em ~19s, sem subir Docker).*
- **Testes de Integração e Aceitação (Níveis 2 e 3):**
  ```powershell
  .\mvnw.cmd verify
  # ou explicitando o perfil Maven:
  .\mvnw.cmd -Pintegration verify
  ```
  *(Failsafe descobre e executa todas as classes `**/*IT.java` contra contêiner PostgreSQL 16 via Testcontainers).*

---

## 3. Diferenciação Rigorosa dos Testes: Legado U1 vs. Persistência U2

| Domínio / Entrega | Suíte / Arquivo | Quantidade | Descrição / Propósito |
|---|---|---|---|
| **Legado AED U1 (C)** | `aed-c/tests/test_lista.c` | 23 asserções | Teste unitário da lista encadeada manual com ponteiros em C. |
| **Legado AED U1 (C)** | `aed-c/tests/test_fila.c` | 31 asserções | Teste unitário da fila FIFO com nós dinâmicos em C. |
| **Legado SO U1 (Python)** | `bench/test_plot_speedup.py` | 7 testes | Validação matemática dos cálculos de speedup e agregações estatísticas. |
| **Legado SO U1 (Python)** | `bench/plot_speedup.py --check` | 8 configs | Checagem de consistência das 40 medições empíricas brutas. |
| **Legado AED U1 (Java)** | `ListaEstoqueTest.java` | 14 testes base | Teste unitário da lista encadeada manual em Java (`ListaEstoque`). |
| **Legado AED U1 (Java)** | `FilaRequisicaoTest.java` | 12 testes | Teste unitário da fila FIFO manual em Java (`FilaRequisicoes`). |
| **Legado SO U1 (Java)** | `DuplicateDetectionServiceTest.java` | 11 testes | Testes de concorrência com threads virtuais e fixas. |
| **Legado SO U1 (Java)** | `DuplicateControllerTest.java` | 5 testes | Testes do endpoint de geração de carga e detecção de duplicatas. |
| **Legado SO U1 (Java)** | `FuzzyMatcherTest.java` | 5 testes | Testes do algoritmo de distância de Levenshtein. |
| **Linha de Base Spring** | `RotaVitalApplicationTest.java` | 1 teste | Teste de carga do contexto Spring Boot padrão. |
| **Caracterização U1 (Java)** | `EstoqueServiceCaracterizacaoTest.java` | 12 testes | Caracterização do comportamento em memória sob perfil `u1`. |
| **Caracterização U1 (Java)** | `EstoqueControllerCaracterizacaoTest.java` | 21 testes | Caracterização REST da API legada sob perfil `u1`. |
| **Novos TDD U2 — H1/H2/H3 (Java)** | `ListaEstoqueStorageTest.java` | 20 testes | Adaptador em memória com LIFO, remoção, UTF-8 e limites. |
| **Novos TDD U2 — H1/H2/H3 (Java)** | `JdbcEstoqueStorageTest.java` | 25 testes | Adaptador JDBC, subqueries SQL, `@Transactional`, sanitização. |
| **Novos TDD U2 — H1/H2/H3 (Java)** | `EstoqueServiceTest.java` | 16 testes | Camada de serviço, injeção de dependência e fail-fast sem fallback. |
| **Novos TDD U2 — H1/H2/H3 (Java)** | `EstoqueControllerTest.java` | 19 testes | Controller REST, envelopes JSON, status HTTP e blindagem. |
| **Novos TDD U2 — DTO (Java)** | `BolsaRequestDTOTest.java` | 17 testes | Resiliência de tipos (String vs Double), `NaN`/`Infinity`, ISO-8601. |
| **Novos TDD U2 — Perfis (Java)** | `EstoqueService*ProfileInjectionTest.java` | 2 testes | Comprovação de injeção correta nos perfis `u1` e `infra`. |
| **Novos TDD U2 — Expansões** | `ListaEstoqueTest.java` (+3) | 3 testes | Remoção de duplicatas no meio da lista encadeada e `removerPorId`. |
| **Novos TDD U2 — Config/Deploy (Java)** | `DatabaseConfigTest.java` | 18 testes | Blindagem de `DATABASE_URL`, portas TCP (1..65535), IPv6 e fallbacks. |
| **Novos TDD U2 — Integração IT** | `JdbcEstoqueStorageIT.java` | 19 testes | Operações DML diretas contra PostgreSQL 16 Real via Testcontainers. |
| **Novos TDD U2 — Persistência IT** | `BolsaPersistidaIT.java` | 3 testes | Comprovação de sobrevivência após destruição e recriação de contexto. |
| **Novos TDD U2 — Aceitação IT** | `EstoqueControllerIT.java` | 17 testes | Aceitação HTTP de ponta a ponta com banco real (PostgreSQL). |

**Totais Auditados:**
- Testes Unitários e de Caracterização Java (Surefire): **201 testes aprovados** (0 falhas, 0 erros, 0 pulados).
- Métodos de Teste de Integração e Aceitação Java (Failsafe): **39 métodos em 3 classes IT** executados contra Testcontainers.
- Testes em C (AED U1): **54 asserções aprovadas** com sanitizers.
- Testes em Python (SO U1): **7 testes unitários aprovados** + 8 configurações auditadas.

---

## 4. Matriz Completa de Rastreabilidade Red → Green

A tabela abaixo documenta o ciclo de desenvolvimento das histórias de persistência da Release 1:

| História / Tarefa | Classe / Teste Alvo | Comando | Falha Esperada (Red) | Commit Red | Implementação / Solução (Green) | Commit Green / Auditoria | Resultado Auditado |
|---|---|---|---|---|---|---|---|
| **U1-Caract.** | `EstoqueServiceCaracterizacaoTest`<br>`EstoqueControllerCaracterizacaoTest` | `mvnw test` | Caracterização inicial: ausência de testes formais para API legada. | `e3b6073` | Criação de 33 testes de caracterização do comportamento em memória sob perfil `u1`. | `e3b6073` | **33 testes aprovados** |
| **H1 / T1.1** | `ListaEstoqueStorageTest`<br>`deveSalvarERecuperarBolsaEmMemoria` | `mvnw test -Dtest=ListaEstoqueStorageTest` | Erro de compilação: classes `EstoqueStorage` e `ListaEstoqueStorage` inexistentes. | `2525767` | Criação da interface `EstoqueStorage` e implementação de `ListaEstoqueStorage` delegando para `ListaEstoque`. | `07c44a7` | **10 testes aprovados** |
| **H1 / T1.2** | `JdbcEstoqueStorageTest`<br>`deveInserirBolsaNoPostgresComSucesso` | `mvnw test -Dtest=JdbcEstoqueStorageTest` | Erro de compilação: classe `JdbcEstoqueStorage` inexistente. | `a80debc` | Implementação de `JdbcEstoqueStorage` com Spring JDBC (`JdbcTemplate`), SQL `INSERT INTO bolsas` e perfil `@Profile("infra")`. | `8aa3432` | **10 testes aprovados** |
| **H1 / T1.3** | `EstoqueServiceTest`<br>`deveDelegarSalvarParaStorage` | `mvnw test -Dtest=EstoqueServiceTest` | Falha de asserção: `EstoqueService` acoplado diretamente à `ListaEstoque` em memória. | `3cd4564` | Refatoração de `EstoqueService` com injeção de dependência de `EstoqueStorage` e fail-fast sem fallback. | `4b880af` | **8 testes aprovados** |
| **H1 / T1.4** | `BolsaRequestDTOTest`<br>`EstoqueControllerTest` | `mvnw test` | Deserialização JSON falhava com `volumeMl` numérico literal das fixtures oficiais. | `54d83c0` | Criação de `BolsaRequestDTO` resiliente com suporte a String e double, validação de campos e integração no controller. | `badd2a0` | **25 testes aprovados** |
| **H1 / T1.5** | `JdbcEstoqueStorageIT`<br>`EstoqueControllerIT`<br>`BolsaPersistidaIT` | `mvnw verify` | Testes de integração e persistência de H1 ainda não existiam no projeto. | `2d0be08` | Implementação da suíte de integração com Testcontainers PostgreSQL 16 e teste de reinício de contexto Spring. | `2d0be08` | **Suíte H1 aprovada** |
| **H1-Auditoria** | Bateria adversarial de robustez e limites DDL | `mvnw test` | Injeção incorreta nos perfis, vulnerabilidade a strings longas ($>50$) e `NaN`. | `9ce52f0`<br>`aff2511`<br>`2fa1483` | Adição de `@Autowired`, validação de limites de caracteres (`VARCHAR(50)`), sanitização e trim. | `9811ba9`<br>`6ee328a`<br>`3763d86` | **120 testes aprovados** |
| **H2 / T2.1** | `ListaEstoqueStorageTest`<br>`deveListarBolsasEmOrdemLIFO` | `mvnw test -Dtest=ListaEstoqueStorageTest` | Erro de compilação: método `List<Bolsa> listar()` ausente na interface `EstoqueStorage`. | `d5157f5` | Implementação de `listar()` em `ListaEstoqueStorage` retornando cópia defensiva na ordem LIFO. | `1a1589f` | **12 testes aprovados** |
| **H2 / T2.2** | `JdbcEstoqueStorageTest`<br>`deveListarBolsasOrdenadasPorIdSeqDesc` | `mvnw test -Dtest=JdbcEstoqueStorageTest` | Falha de compilação e teste unitário: `listar()` ausente em `JdbcEstoqueStorage`. | `9f2d85b` | Implementação de `listar()` em `JdbcEstoqueStorage` com query `SELECT ... FROM bolsas ORDER BY id_seq DESC`. | `38dd729` | **16 testes aprovados** |
| **H2 / T2.3** | `EstoqueServiceTest`<br>`EstoqueControllerTest` | `mvnw test` | Endpoint `GET /api/estoque` não utilizava envelope padronizado `{"bolsas": [...], "tamanho": N}`. | `f595f60` | Integração de `listarEstoque()` no serviço e refatoração do controller com tamanho determinístico `lista.size()`. | `25960c8` | **28 testes aprovados** |
| **H2 / T2.4** | `JdbcEstoqueStorageIT`<br>`EstoqueControllerIT`<br>`BolsaPersistidaIT` | `mvnw verify` | Ausência de testes de aceitação com as 5 fixtures canônicas e teste de listagem pós-reinício. | `72de699` | Implementação de `devePreservarListagemEOrdemLIFOAposReinicioContexto` e aceitação com as 5 fixtures. | `72de699` | **Suíte H2 aprovada** |
| **H2-Auditoria** | Auditoria adversarial R1, R2 e R3 | `mvnw test` | Skew concorrente no controller, falta de teste com duplicata mais recente e valores `NaN`/`Infinity`. | `906a2cc`<br>`84b1a25` | Blindagem de volume contra não-finitos, salvaguardas de nulidade e teste das 5 fixtures canônicas no perfil `u1`. | `8860c01` | **151 testes aprovados** |
| **H3 / T3.1** | `ListaEstoqueStorageTest`<br>`deveRemoverBolsaComSucesso` | `mvnw test -Dtest=ListaEstoqueStorageTest` | Verificação do comportamento LIFO da remoção em memória da cabeça para cauda. | `0bb4240` | Delegação para `listaEstoque.remover(id.trim())` já implementada na lista encadeada da U1. | `0bb4240` | **14 testes aprovados (Green direto)** |
| **H3 / T3.2** | `JdbcEstoqueStorageTest`<br>`deveRemoverOcorrenciaMaisRecenteDeBolsaDuplicada` | `mvnw test -Dtest=JdbcEstoqueStorageTest` | Falha comprovada: `UnsupportedOperationException: Pendente para Etapa 5 (H3)` em 5 testes. | `2b07ecc` | Implementação de `removerPorId` com subquery atômica `DELETE FROM bolsas WHERE id_seq = (SELECT id_seq ... LIMIT 1)` e `@Transactional`. | `9a4b69b` | **23 testes aprovados** |
| **H3 / T3.3** | `EstoqueServiceTest`<br>`EstoqueControllerTest` | `mvnw test` | Erro de compilação: método `removerBolsa(id)` inexistente em `EstoqueService`. | `49d266f` | Adição de `removerBolsa` em `EstoqueService` e integração de resposta HTTP 200/404 em `EstoqueController`. | `26b19d9` | **30 testes aprovados** |
| **H3 / T3.4** | `JdbcEstoqueStorageIT`<br>`BolsaPersistidaIT`<br>`EstoqueControllerIT` | `mvnw verify` | Ausência de comprovação de persistência da remoção após encerramento e recriação do contexto Spring. | `9ac7c3b` | Implementação de `deveManterRemocaoAposReinicioContexto` em `BolsaPersistidaIT` e testes de aceitação HTTP. | `9ac7c3b` | **Suíte H3 aprovada** |
| **H3-Auditoria** | Auditoria adversarial R1, R2 e R3 | `mvnw test` | Ausência de teste de remoção no meio da lista, UTF-8, limite de 50 caracteres e tripla duplicata. | `b8cab3c`<br>`c59eeca`<br>`1350bd3` | Adição de `removerPorId` em `ListaEstoque`, testes defensivos de acentuação, limite de 50 chars e isolamento de prefixos. | `7680687`<br>`1350bd3` | **183 testes aprovados** |

---

## 5. Roteiro Consolidado de Demonstração da Persistência do E1

A comprovação da persistência durável no PostgreSQL através do **Docker Compose** e do teste de integração **`BolsaPersistidaIT`** fundamenta-se nos seguintes passos estruturais:

### 5.1. Roteiro Operacional via Docker Compose
1. **Subida Limpa da Infraestrutura:**
   ```bash
   docker compose up -d --build
   docker compose ps
   ```
   *Ambos os serviços (`rotavital-db` e `rotavital-api`) iniciam saudáveis (`healthy`), com Flyway aplicando a migração `V1__criar_tabela_bolsas.sql` automaticamente.*

2. **Cadastro de Bolsa pela API REST (Perfil Infra):**
   ```bash
   curl -X POST http://localhost:8080/api/estoque \
     -H "Content-Type: application/json" \
     -d '{"id":"BOLSA-DEMO-01","tipoSanguineo":"AB+","volumeMl":450.0,"dataColeta":"2026-10-17","validade":"2026-12-17","doador":"Maria Silva"}'
   ```
   *Resposta esperada: HTTP 200 OK com payload `{"mensagem": "Bolsa adicionada ao estoque", "tamanho": 1}`.*

3. **Confirmação Direta no Banco PostgreSQL Real:**
   ```bash
   docker compose exec -e PGPASSWORD=rotavital db psql -U rotavital -d rotavital -c "SELECT id_seq, id, tipo_sanguineo, volume_ml, doador FROM bolsas;"
   ```
   *Evidência observável: Registro persistido fisicamente na tabela com `id_seq = 1`.*

4. **Reinício do Processo da Aplicação:**
   ```bash
   docker compose restart api
   ```
   *O contêiner da API é reiniciado (simulando crash ou reboot). Em seguida, consulta-se o endpoint:*
   ```bash
   curl -s http://localhost:8080/api/estoque/BOLSA-DEMO-01
   ```
   *Resposta: HTTP 200 OK com todos os dados íntegros recuperados do banco.*

5. **Encerramento dos Contêineres Preservando o Volume Nomeado:**
   ```bash
   docker compose down
   # ATENÇÃO: Nunca utilizar a flag '-v', pois ela destruiria o volume nomeado.
   docker compose up -d
   ```
   *Após a recriação completa dos contêineres a partir do volume persistente `rotavital-postgres-data`:*
   ```bash
   curl -s http://localhost:8080/api/estoque
   ```
   *Resposta: Envelope JSON `{"bolsas": [{"id":"BOLSA-DEMO-01", ...}], "tamanho": 1}` preservado integralmente.*

### 5.2. Vínculo com a Suíte Automatizada (`BolsaPersistidaIT`)
O roteiro manual acima é comprovado matematicamente e sem intervenção humana pelos três métodos de teste em `BolsaPersistidaIT`:
- **`devePersistirBolsaAposReinicioContexto` (H1):** Grava a bolsa, executa `applicationContext.close()`, recria um novo contexto Spring conectado ao mesmo contêiner PostgreSQL e valida campo a campo a igualdade dos dados.
- **`devePreservarListagemEOrdemLIFOAposReinicioContexto` (H2):** Grava duplicatas e múltiplas bolsas, destrói o contexto, recria e atesta a ordem LIFO rigorosamente preservada.
- **`deveManterRemocaoAposReinicioContexto` (H3):** Exclui uma ocorrência de duplicata, destrói o contexto, atesta no segundo contexto que a ocorrência excluída não reapareceu e que a duplicata sobrevivente persiste, e remove a duplicata restante validando a ausência permanente em um terceiro contexto recriado.

---

## 6. Conclusão da Etapa 6 e Estado da Esteira

Com a conclusão da Etapa 6:
- O Maven possui separação auditada entre testes unitários rápidos (`.\mvnw.cmd test` via Surefire) e testes de integração com banco real (`.\mvnw.cmd -Pintegration verify` via Failsafe).
- A esteira de CI (`.github/workflows/ci.yml`) possui 5 gates estritos sem contornos permissivos (`continue-on-error` ou `|| true`).
- A demonstração de persistência do E1 está documentada e vinculada à suíte de testes de integração.
- A **Etapa 7** (Branches, GitHub Flow e Deploy Automatizado no Render) permanece expressamente **Pendente**.
