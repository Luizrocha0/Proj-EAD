# 🩸 Rota Vital — Banco de Sangue

> Backend Spring Boot para o Projeto Integrador **Rota Vital** — gestão inteligente de banco de sangue com estruturas de dados implementadas manualmente em C e Java.

**Equipe:** Eduardo Borges · Luiz Henrique Rocha · Eliziane Mota · Pedro Iranildo · Ricardo Severiano  
**Disciplinas:** Algoritmos e Estruturas de Dados (AED) + Infraestrutura de Software (SO)  
**Semestre:** 2026.2

---

## 📋 Índice

- [Visão Geral](#visão-geral)
- [Requisitos](#requisitos)
- [Build e Teste (Java)](#build-e-teste-java)
- [Estruturas AED em C](#estruturas-aed-em-c)
- [Executar a API](#executar-a-api)
- [Endpoints REST](#endpoints-rest)
- [Deploy](#deploy)
- [Estrutura do Projeto](#estrutura-do-projeto)

---

## Visão Geral

O Rota Vital simula a gestão de um banco de sangue com:

- **Lista encadeada de estoque** — bolsas armazenadas em lista simplesmente encadeada com nós próprios
- **Fila FIFO de requisições** — requisições emergenciais atendidas por ordem de chegada
- **Pilha (LIFO)** — não foi implementada pois não existe histórico de operações no escopo atual.
- **Equivalência C ↔ Java** — mesma lógica implementada nas duas linguagens, reproduzindo os valores das fixtures (JSONs sintéticos) embutidos diretamente no código para testes (foram aprovados 27 testes Java, 19 testes C da lista e 29 testes C da fila).
- **API REST** — endpoints Spring Boot que consomem as estruturas AED reais
- **CI/CD** — GitHub Actions com testes C (AddressSanitizer), testes Java, Docker build e deploy

---

## Requisitos

| Ferramenta | Versão mínima |
|---|---|
| **Java JDK** | 21+ |
| **Maven** | 3.9+ (via Maven Wrapper incluso) |
| **GCC** | 11+ (para compilar as estruturas C) |
| **Docker** | 24+ (opcional, para containerização) |

---

## Build e Teste (Java)

```bash
# Definir JAVA_HOME se necessário
export JAVA_HOME=/caminho/para/jdk-21

# Compilar e executar todos os testes
./mvnw test

# Compilar sem testes (gerar JAR)
./mvnw package -DskipTests
```

**No Windows:**

```powershell
$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-21"
.\mvnw.cmd test
```

### Resultado esperado

```
Tests run: 27, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

---

## Estruturas AED em C

As estruturas em C ficam no diretório `aed-c/` e são compiladas/testadas separadamente com GCC e AddressSanitizer:

```bash
cd aed-c

# Compilar e executar todos os testes (com sanitizers de memória)
make test

# Limpar binários de teste
make clean
```

### Resultado esperado

```
=== Testes da Lista Encadeada (AED U1 — C) ===
  OK: Lista recem-criada tem tamanho 0
  OK: Consulta em lista vazia retorna NULL
  ...
=== Resultado: 19/19 testes passaram ===
SUCESSO: Todos os testes passaram sem erros de memoria!

=== Testes da Fila FIFO (AED U1 — C) ===
  OK: Fila recem-criada tem tamanho 0
  ...
=== Resultado: 29/29 testes passaram ===
SUCESSO: Todos os testes passaram sem erros de memoria!
```

> O `Makefile` compila com `-fsanitize=address,undefined` para detectar automaticamente vazamentos de memória (memory leaks), buffer overflows e comportamento indefinido.

---

## Executar a API

```bash
./mvnw spring-boot:run
```

A aplicação sobe na porta **8080**. Verifique com:

```bash
curl http://localhost:8080/actuator/health
# {"status":"UP"}
```

---

## Endpoints REST

### Estoque de Bolsas

| Método | Endpoint | Descrição |
|---|---|---|
| `POST` | `/api/estoque` | Adiciona bolsa ao estoque |
| `GET` | `/api/estoque` | Lista todas as bolsas |
| `GET` | `/api/estoque/{id}` | Consulta bolsa por ID |
| `DELETE` | `/api/estoque/{id}` | Remove bolsa do estoque |

**Exemplo — Adicionar bolsa:**

```bash
curl -X POST http://localhost:8080/api/estoque \
  -H "Content-Type: application/json" \
  -d '{
    "id": "BOLSA-001",
    "tipoSanguineo": "O+",
    "volumeMl": "450.0",
    "dataColeta": "2026-09-01",
    "validade": "2026-11-01",
    "doador": "João Silva"
  }'
```

### Fila de Requisições

| Método | Endpoint | Descrição |
|---|---|---|
| `POST` | `/api/requisicoes` | Enfileira requisição |
| `POST` | `/api/requisicoes/atender` | Desenfileira e atende próxima (FIFO) |
| `GET` | `/api/requisicoes/proxima` | Consulta próxima sem remover |
| `GET` | `/api/requisicoes` | Lista todas na fila |

**Exemplo — Enfileirar requisição:**

```bash
curl -X POST http://localhost:8080/api/requisicoes \
  -H "Content-Type: application/json" \
  -d '{
    "id": "REQ-001",
    "hospitalId": "HOSP-CENTRAL",
    "tipoSanguineo": "O+",
    "quantidadeBolsas": "2",
    "prioridade": "EMERGENCIA"
  }'
```

### Health Check

| Método | Endpoint | Descrição |
|---|---|---|
| `GET` | `/actuator/health` | Status da aplicação |
| `GET` | `/actuator/info` | Informações do projeto |

### Detecção de Duplicatas (Paralelismo)

| Método | Endpoint | Descrição |
|---|---|---|
| `GET` | `/api/duplicates/detect` | Encontra requisições duplicadas (fuzzy matching) |

**Exemplo — Testar com 8000 registros em 8 threads (platform):**

```bash
curl "http://localhost:8080/api/duplicates/detect?size=8000&threads=8&mode=platform"
```

---

## Benchmark de Paralelismo (Threads)

A detecção de duplicatas compara pares de textos com distância de Levenshtein. O cálculo em memória custa O(n² · L²), para `n` registros de comprimento limitado por `L`, e permite distribuir pares independentes entre threads.

A série publicada em **06/10/2026** usa exclusivamente [bench/measurements.csv](bench/measurements.csv): 8.000 e 16.000 registros, modo `platform`, 1, 2, 4 e 8 threads, com 5 repetições por configuração. As 40 execuções registram `SUCCESS`, com 3.198.218 pares duplicados para 8.000 registros e 12.801.507 para 16.000, iguais entre repetições e quantidades de threads.

O script [bench/measure.ps1](bench/measure.ps1) prevê um aquecimento com 8.000 registros e uma thread antes da coleta. A tabela usa a média de `responseTimeMs`, medida pelo cliente ao redor de `Invoke-RestMethod`; inclui a chamada HTTP e seu processamento até o retorno. `processingTimeMs` é o tempo interno de detecção e constitui outra métrica. O speedup é `T1 / Tt`, usando médias para o mesmo tamanho.

| Tamanho (n) | Threads | Tempo Médio HTTP (ms) | Speedup |
|---|---|---|---|
| 8000 | 1 | 23883.00 | 1.00x |
| 8000 | 2 | 18925.60 | 1.26x |
| 8000 | 4 | 11105.20 | 2.15x |
| 8000 | 8 | 7209.80 | 3.31x |
| 16000 | 1 | 97867.00 | 1.00x |
| 16000 | 2 | 81400.00 | 1.20x |
| 16000 | 4 | 69518.20 | 1.41x |
| 16000 | 8 | 41297.40 | 2.37x |

Para recalcular [os dados agregados](bench/measurements_agg.csv) e os gráficos a partir dos dados brutos, execute na raiz do projeto com Python 3.12+ e `matplotlib`:

```bash
python bench/plot_speedup.py
python bench/plot_speedup.py --check
```

Para instalar as versões fixadas em `bench/requirements.txt` (`matplotlib==3.11.2` e `reportlab==4.4.9`), use um ambiente isolado. Exemplo no PowerShell:

```powershell
python -m venv .venv-bench
.\.venv-bench\Scripts\python.exe -m pip install -r bench/requirements.txt
.\.venv-bench\Scripts\python.exe bench/plot_speedup.py
```

Consulte [medições e limites](docs/medicoes.md), [justificativa](docs/justificativa.md), [análise](docs/analise.md) e [relatório PDF](docs/RotaVital_Threads_Relatorio.pdf). O gráfico está em [bench/speedup.png](bench/speedup.png), com cópia em [docs/speedup.png](docs/speedup.png). Esta série não contém medições de virtual threads; o CSV não identifica hardware nem a versão exata do JDK usado.

---

## Deploy

### Docker

```bash
# Build da imagem
docker build -t rota-vital .

# Executar
docker run -p 8080:8080 rota-vital

# Health check
curl http://localhost:8080/actuator/health
```

### CI/CD (GitHub Actions)

O pipeline em `.github/workflows/ci.yml` executa automaticamente:

1. **test-c** — Compila e testa estruturas C com AddressSanitizer
2. **test-java** — Executa JUnit 5 com Maven
3. **build-java** — Gera JAR do Spring Boot
4. **build-docker** — Constrói imagem Docker
5. **health-check** — Verifica `/actuator/health` (apenas na `main`)
6. **deploy-render** — Deploy automático no Render (apenas na `main`)
7. **check-benchmark** — Confere os testes de cálculo e a equivalência entre medições brutas e agregadas; bloqueia o build Docker em caso de divergência

### Render

Para deploy no Render, configure os secrets no GitHub:

- `RENDER_DEPLOY_HOOK_URL` — URL do deploy hook do Render
- `RENDER_APP_URL` — URL pública da aplicação

**URL Pública (Produção):** [https://rota-vital.onrender.com](https://rota-vital.onrender.com) (ou a URL configurada)

---

## Estrutura do Projeto

```
rota-vital/
├── pom.xml                           # Spring Boot 3.3.3 + Java 21
├── mvnw / mvnw.cmd                   # Maven Wrapper
├── Dockerfile                        # Multi-stage (JDK → JRE Alpine)
├── README.md                         # Este arquivo
│
├── src/main/java/com/rotavital/
│   ├── RotaVitalApplication.java     # Ponto de entrada
│   ├── controller/
│   │   ├── EstoqueController.java    # REST — estoque de bolsas
│   │   └── RequisicaoController.java # REST — fila de requisições
│   ├── service/
│   │   ├── EstoqueService.java       # Lógica de estoque (usa ListaEstoque)
│   │   └── RequisicaoService.java    # Lógica de fila (usa FilaRequisicoes)
│   ├── domain/
│   │   ├── Bolsa.java                # Entidade: bolsa de sangue
│   │   └── Requisicao.java           # Entidade: requisição emergencial
│   └── aed/u1/
│       ├── ListaEstoque.java         # Lista encadeada — nós próprios
│       ├── FilaRequisicoes.java      # Fila FIFO — nós próprios
│       ├── NoBolsa.java              # Nó da lista
│       └── NoRequisicao.java         # Nó da fila
│
├── src/test/java/com/rotavital/
│   ├── RotaVitalApplicationTest.java # Smoke test Spring Boot
│   └── aed/u1/
│       ├── ListaEstoqueTest.java     # 14 testes — mesmas fixtures
│       └── FilaRequisicaoTest.java   # 12 testes — mesmas fixtures
│
├── aed-c/                            # Módulo C (equivalente didático)
│   ├── include/
│   │   ├── bolsa.h                   # Struct Bolsa
│   │   ├── requisicao.h              # Struct Requisicao + enum Prioridade
│   │   ├── lista.h                   # Contrato da lista encadeada
│   │   └── fila.h                    # Contrato da fila FIFO
│   ├── src/u1/
│   │   ├── lista.c                   # Implementação com malloc/free
│   │   └── fila.c                    # Implementação com malloc/free
│   ├── tests/
│   │   ├── test_lista.c              # Testes — mesmas fixtures
│   │   └── test_fila.c              # Testes — mesmas fixtures
│   └── Makefile                      # GCC + AddressSanitizer + UBSan
│
├── fixtures/                         # Dados sintéticos compartilhados C/Java
│   ├── bolsas_u1.json
│   ├── requisicoes_u1.json
│   └── resultados_esperados_u1.json
│
├── docs/
│   └── equivalencia-u1.md            # Tradução comentada C ↔ Java
│
└── .github/workflows/ci.yml          # Pipeline CI/CD completo
```

---

## Licença

Projeto acadêmico — uso exclusivo para o Projeto Integrador 2026.2.
