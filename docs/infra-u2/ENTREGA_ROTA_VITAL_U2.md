# 🩸 Dossiê Executivo de Entrega — Unidade 2 (Desafios PI — Parte 1)

> **Projeto:** Rota Vital — Banco de Sangue Inteligente  
> **Disciplina:** Infraestrutura de Software (Sistemas Operacionais) & Algoritmos e Estruturas de Dados (AED)  
> **Semestre:** 2026.2 | **Prazo Oficial:** 20/10/2026  
> **Estado da Entrega:** ✅ **100% CONCLUÍDO E HOMOLOGADO EM PRODUÇÃO**

---

## 1. Identificação da Equipe e Funções

| Integrante | Função no Projeto | Responsabilidades Principais |
|---|---|---|
| **Eduardo Borges** | Desenvolvedor Backend | Camada de persistência relacional, JDBC puro, migrações Flyway e adaptadores de armazenamento |
| **Luiz Henrique Rocha** | Tech Leader | Arquitetura de software, portas/adaptadores, perfis Spring (`u1` e `infra`) e governança técnica |
| **Eliziane Mota** | QA Engineer | Condução dos ciclos TDD (Red $\to$ Green), critérios de aceite e automação da pirâmide de testes |
| **Pedro Iranildo** | DevOps Engineer | Dockerfile multi-stage, compose.yaml, esteira CI/CD em DAG e infraestrutura de deploy |
| **Ricardo Severiano de Souza Filho** | Product Owner | Backlog, story map, rastreabilidade dos requisitos acadêmicos e consolidação documental |

---

## 2. Links Oficiais para Avaliação

| Recurso | Endereço URL | Finalidade de Avaliação |
|---|---|---|
| **Repositório GitHub** | [https://github.com/Luizrocha0/Proj-EAD](https://github.com/Luizrocha0/Proj-EAD) | Código-fonte completo, histórico e documentação |
| **Pull Request de Entrega** | [https://github.com/Luizrocha0/Proj-EAD/pull/1](https://github.com/Luizrocha0/Proj-EAD/pull/1) | PR #1 homologado e mesclado (*merged*) na branch `main` |
| **Aplicação Pública (Render)** | [https://rota-vital.onrender.com](https://rota-vital.onrender.com) | Instância no ar executando na nuvem sob perfil `infra` |
| **Health Check (Actuator)** | [https://rota-vital.onrender.com/actuator/health](https://rota-vital.onrender.com/actuator/health) | Verificação de integridade operacional (`{"status":"UP"}`) |
| **Endpoint de Estoque REST** | [https://rota-vital.onrender.com/api/estoque](https://rota-vital.onrender.com/api/estoque) | Listagem pública com envelope padronizado (HTTP 200 OK) |
| **Esteira CI/CD (Actions)** | [https://github.com/Luizrocha0/Proj-EAD/actions](https://github.com/Luizrocha0/Proj-EAD/actions) | Execução 100% verde de todos os 6 gates em DAG na `main` |

---

## 3. Síntese Técnica das Entregas

### Bloco E1 — Containerização e Docker Compose
- **Dockerfile Multi-Stage Otimizado:** Estágio de builder que executa obrigatoriamente os testes unitários sem `-DskipTests` (`./mvnw package -DskipITs -q`), impedindo que código quebrado gere imagens. Runtime enxuto baseado em `eclipse-temurin:21-jre-alpine` com usuário non-root (`rotavital:rotavital`, UID 10001).
- **Consumo de Sinais POSIX:** O entrypoint consome formalmente `exec java $JAVA_OPTS -jar app.jar`, assumindo o PID 1 e respondendo a sinais de `SIGTERM` para encerramento gracioso (*graceful shutdown*).
- **Orquestração Local (`compose.yaml`):** Serviços `api` e `db` interligados na rede bridge isolada `rotavital-network`, com volume nomeado persistente `rotavital-postgres-data` e healthchecks nativos encadeados via `pg_isready`.
- **Blindagem de Segredos:** Modelo `.env.example` fornecido com regras estritas no `.gitignore` e `.dockerignore`.

### Bloco E2 — TDD e Persistência Relacional
- **Arquitetura Hexagonal Mínima:** Interface `EstoqueStorage` segregando a implementação em memória original (`ListaEstoqueStorage` com perfil `u1`) da persistência relacional (`JdbcEstoqueStorage` com perfil `infra`).
- **Proibição de Fallback Silencioso:** Sob o perfil `infra`, a indisponibilidade do banco de dados gera falha ruidosa imediata, sem redirecionar operações para a memória.
- **História H1 (Cadastro e Recuperação):** Gravação com todos os campos no PostgreSQL 16 e recuperação de bolsas via `BolsaRequestDTO` resiliente (aceita volume como número ou string).
- **História H2 (Listagem e Envelope):** Envelope JSON padronizado `{"bolsas": [...], "tamanho": N}`, ordenação LIFO estrita (`ORDER BY id_seq DESC`) e preservação de duplicatas.
- **História H3 (Remoção Persistente):** Deleção atômica da ocorrência mais recente sob IDs repetidos via subquery SQL:
  $$\text{DELETE FROM bolsas WHERE id\_seq = (SELECT id\_seq FROM bolsas WHERE id = ? ORDER BY id\_seq DESC LIMIT 1)}$$
  Retorno HTTP 200/404 padronizado e sobrevivência da exclusão comprovada pós-reinício em `BolsaPersistidaIT`.

### Bloco E3 — CI/CD, Deploy no Render e Governança
- **Esteira CI/CD em DAG:** 6 gates no GitHub Actions (`.github/workflows/ci.yml`) sem contornos permissivos (`continue-on-error: true` e `|| true` proibidos).
- **Deploy Hook Seguro com Commit SHA:** Gatilho condicionado ao sucesso estrito dos 5 gates anteriores na branch `main`, enviando o SHA validado via `ref=${{ github.sha }}`.
- **Governança de Rollback em 3 Vias:** Procedimento documentado via webhook com SHA anterior, reversão manual no dashboard do Render e `git revert` com PR.
- **Proteção Flyway (Expand/Contract):** Evolução aditiva de esquema com `spring.flyway.clean-disabled=true` ativado em produção.
- **Proteção Remota de Branch:** Regra ativada na branch `main` no GitHub (`protected: true`) exigindo aprovação dos 5 status checks do CI antes de qualquer merge.

---

## 4. Baterias de Testes e Validação Empírica

| Suíte de Testes | Tecnologia / Ferramenta | Quantidade | Resultado |
|---|---|:---:|:---:|
| **Testes Unitários Java** | Maven Surefire / JUnit 5 | **201** | ✅ **201/201 APROVADOS** (0 falhas, 0 erros) |
| **Testes de Integração & Aceitação** | Maven Failsafe + Testcontainers (PostgreSQL 16) | **39** | ✅ **39/39 APROVADOS** (banco real em contêiner) |
| **Persistência Pós-Reinício** | `BolsaPersistidaIT` (Spring Context Restart) | **3** | ✅ **APROVADO** (sobrevivência comprovada) |
| **Módulo AED em C (U1)** | GCC + AddressSanitizer + UBSan | **54 asserções** | ✅ **54/54 APROVADAS** (zero vazamentos de memória) |
| **Benchmarks SO em Python (U1)** | Python `unittest` + Validação Estatística | **7 testes** | ✅ **7/7 APROVADOS** (40 medições validadas) |
| **Smoke Test Containerizado** | Docker Compose + cURL (CRUD de 9 etapas) | **9 passos** | ✅ **APROVADO** (Actuator UP + 200/400/404) |

---

## 5. Tabela das 12 Decisões Arquiteturais Justificadas

| # | Decisão Arquitetural | Alternativa Considerada | Justificativa Técnica Concreta |
|:---:|---|---|---|
| **1** | **Eclipse Temurin 21 JRE Alpine** | Ubuntu / Debian slim | Imagem oficial LTS mínima (< 180MB), reduzindo superfície de ataque e acelerando o deploy no Render. |
| **2** | **Build Multi-Stage sem `-DskipTests`** | Build único ou skip | Garante que testes unitários executem no build da imagem, impedindo código defeituoso em produção. |
| **3** | **PostgreSQL 16 Oficial Alpine** | H2 em memória | Paridade total de produção (12-Factor App) e compatibilidade com Testcontainers e Render PostgreSQL. |
| **4** | **Spring Data JDBC puro (`JdbcTemplate`)** | Spring Data JPA / Hibernate | Controle determinístico sobre o SQL executado, permitindo a subquery atômica LIFO sem overhead de proxies ou cache. |
| **5** | **Migrações com Flyway** | `ddl-auto: update` / Scripts manuais | Versionamento declarativo e auditável do esquema do banco, com validação automática de checksums na inicialização. |
| **6** | **Volume Nomeado `rotavital-postgres-data`** | Bind mount no host | Persistência gerenciada pelo Docker Engine, imune a permissões locais de arquivos no host Windows/Linux. |
| **7** | **Rede Bridge Isolada `rotavital-network`** | Rede `host` / Rede default | Isolamento de segurança: a API e o banco comunicam-se por DNS interno sem expor portas do PostgreSQL para a internet. |
| **8** | **Pirâmide em 3 Níveis (Surefire vs Failsafe)** | Execução única mista | Nível 1 roda em segundos sem Docker; Níveis 2 e 3 validam banco real via Testcontainers com fail-fast estrito. |
| **9** | **GitHub Flow com Branch de Trabalho** | Push direto na `main` | Commits atômicos rastreáveis, abertura de PR com execução da esteira e histórico limpo e auditável. |
| **10** | **Pipeline CI/CD em DAG com 6 Gates** | Pipeline monolítico linear | Paralelismo inicial entre Python, C e Java unitário (Fail-Fast), convergindo para Testcontainers, Smoke Test e Deploy. |
| **11** | **Deploy Hook no Render com `ref=${{ github.sha }}`** | Deploy contínuo automático | Garante que o Render publique estritamente o commit SHA aprovado pelos 5 gates do CI, evitando commits intermediários. |
| **12** | **Segredos em Cofres (GitHub Secrets e `.env`)** | Chaves hardcoded no código | Blindagem total contra vazamento de credenciais em conformidade estrita com o 12-Factor App e LGPD. |

---

## 6. Declaração Transparente de Uso de Inteligência Artificial

Em conformidade com as diretrizes acadêmicas de integridade:
- **Ferramentas Utilizadas:** Google Antigravity / Gemini 2.0.
- **Papel da IA:** Assistente de desenvolvimento para aceleração de testes unitários, automação de scripts de esteira CI/CD e suporte à redação técnica dos relatórios.
- **Supervisão Humana:** Todas as decisões arquiteturais (JDBC puro, chave `id_seq`, subqueries LIFO, separação de perfis `u1` e `infra`) foram definidas e revisadas pela equipe.
- **Validação:** Todos os 201 testes Java, 54 asserções em C e 7 testes Python foram executados localmente e validados de ponta a ponta na esteira remota do GitHub Actions.

---

## 7. Catálogo de Arquivos e Documentação no Repositório

```
docs/
├── evidencias/
│   └── README.md                 # Catálogo detalhado de todas as evidências da U2
├── infra-u2/
│   ├── apresentacao.md           # Roteiro estruturado para apresentação oral em sala
│   ├── checklist.md              # Matriz completa de conformidade para a banca
│   ├── divisao-tarefas.md        # Atribuição formal de responsabilidades por integrante
│   ├── progresso.md              # Registro cronológico detalhado e auditoria final
│   ├── story-map.md              # Story Map, histórias H1, H2, H3 e matriz de 27 testes
│   └── tdd.md                    # Relatório aprofundado dos ciclos Red -> Green -> Refactor
├── equivalencia-u1.md            # Tradução comparada C <-> Java da U1
├── medicoes.md, analise.md       # Documentos de medições e threads de SO (U1)
└── speedup.png                   # Gráfico oficial de speedup de threads da U1
```

---

**Conclusão:** O projeto **Rota Vital** cumpre integralmente todas as exigências conceituais, práticas e de engenharia de software da Unidade 2, encontrando-se plenamente operacional em nuvem e pronto para avaliação acadêmica.
