---
name: rota-vital-aed
description: "Skill especializada para o projeto integrador Rota Vital (AED + SO). Cobre implementação de estruturas de dados em C e Java, integração com Spring Boot, CI/CD e deploy."
category: academic-project
risk: safe
source: self
date_added: "2026-09-28"
tags: "[java, spring-boot, c, data-structures, aed, so, ci-cd, docker]"
---

# Rota Vital — Projeto Integrador AED + SO

## Contexto do Projeto

Backend Spring Boot 3.3.3 + Java 21 + Maven para o projeto integrador **Rota Vital** (banco de sangue).

**Equipe:** Eduardo Borges, Luiz Henrique Rocha, Eliziane Mota, Pedro Iranildo, Ricardo Severiano  
**Escopo:** AED (AV1: 02/10/2026 | AV2: 04/12/2026) + SO  
**Repositório base:** `threads-2.zip` com endpoints REST já funcionais

## Estrutura do Repositório

```
rota-vital/
  pom.xml                         # Spring Boot existente
  mvnw / mvnw.cmd                 # Maven Wrapper CORRIGIDO
  src/main/java/com/rotavital/
    controller/                   # REST controllers
    service/                      # Lógica de negócio
    domain/                       # Bolsa, Requisicao, Hospital
    aed/u1/                       # Lista, Fila e Pilha Java
    aed/u2/                       # Hash, FEFO, Matriz ABO/Rh, Rota
    util/                         # FuzzyMatcher
  src/test/java/com/rotavital/    # JUnit 5 + Spring Boot Test
  aed-c/
    include/                      # Headers C
    src/u1/                       # lista.c, fila.c, pilha.c
    src/u2/                       # hash.c, fefo.c, matriz.c, rota.c
    tests/                        # testes C com mesmas fixtures
    Makefile
  fixtures/                       # Dados sintéticos JSON
  bench/                          # CSV, gráfico e scripts de threads
  docs/threads/                   # PDF e evidências preservadas
  .github/workflows/ci.yml
  Dockerfile
  README.md
```

## AED U1 — Estruturas de Dados (Prazo: 02/10/2026)

### Lista Encadeada de Estoque (C)

```c
// include/lista.h
typedef struct No {
    Bolsa bolsa;
    struct No *proximo;
} No;

typedef struct Lista {
    No *cabeca;
    int tamanho;
} Lista;

Lista* lista_criar(void);
void   lista_inserir(Lista *l, Bolsa b);
Bolsa* lista_consultar(Lista *l, const char *id);
int    lista_remover(Lista *l, const char *id);
void   lista_liberar(Lista *l);          // free de cada nó
```

### Fila de Requisições (C)

```c
// include/fila.h
typedef struct Fila {
    No *cabeca;   // dequeue aqui
    No *cauda;    // enqueue aqui
    int tamanho;
} Fila;

Fila* fila_criar(void);
void  fila_enfileirar(Fila *f, Requisicao r);
int   fila_desenfileirar(Fila *f, Requisicao *out);
void  fila_liberar(Fila *f);
```

### Pilha de Histórico (C — somente se usada de fato)

```c
typedef struct Pilha {
    No *topo;
    int tamanho;
} Pilha;

Pilha* pilha_criar(void);
void   pilha_empilhar(Pilha *p, Evento e);
int    pilha_desempilhar(Pilha *p, Evento *out);
void   pilha_liberar(Pilha *p);
```

### Equivalente Java (mesma lógica, nós próprios)

```java
// aed/u1/ListaEstoque.java
public class ListaEstoque {
    private No cabeca;
    private int tamanho;
    
    public void inserir(Bolsa b) { /* ... */ }
    public Optional<Bolsa> consultar(String id) { /* ... */ }
    public boolean remover(String id) { /* ... */ }
}

class No {
    Bolsa dado;
    No proximo;
}
```

## AED U2 — Algoritmos (Prazo: 04/12/2026)

### Hash Table para índice de bolsas

```c
// Função de hash simples para tipo sanguíneo + Rh
int hash(const char *chave, int capacidade) {
    unsigned long h = 5381;
    int c;
    while ((c = *chave++)) h = ((h << 5) + h) + c;
    return (int)(h % capacidade);
}
```

### FEFO (First Expired, First Out) para seleção de bolsas

```java
// Seleciona bolsa com validade mais próxima do vencimento
public Optional<Bolsa> selecionarFEFO(String tipoSanguineo) {
    return estoque.stream()
        .filter(b -> b.tipoSanguineo().equals(tipoSanguineo))
        .filter(b -> !b.vencida())
        .min(Comparator.comparing(Bolsa::validade));
}
```

### Matriz ABO/Rh de compatibilidade

```java
// Matriz 8x8: [doador][receptor] = compatível?
boolean[][] matrizABO = {
    // O-, O+, A-, A+, B-, B+, AB-, AB+
    {true,  true,  true,  true,  true,  true,  true,  true},  // O-
    {false, true,  false, true,  false, true,  false, true},   // O+
    {false, false, true,  true,  false, false, true,  true},   // A-
    // ...
};
```

### Vizinho mais próximo (rota de entrega)

```java
// Algoritmo guloso O(n²) para grafo com 5-8 hospitais
public List<Hospital> rotaNearestNeighbor(Hospital origem, List<Hospital> destinos) {
    List<Hospital> rota = new ArrayList<>();
    Hospital atual = origem;
    Set<Hospital> visitados = new HashSet<>();
    // ...
}
```

## SO U1 — Checklist de Entregas

- [ ] Endpoint `/api/duplicates/detect` preservado e funcionando
- [ ] `mvnw test` executa do zero (Maven Wrapper corrigido)
- [ ] `.github/workflows/ci.yml` com jobs C e Java separados
- [ ] `Dockerfile` + `Spring Boot Actuator /actuator/health`
- [ ] Deploy no Render com URL pública
- [ ] README com build, teste, execução, benchmark e deploy

## Compilação C com Sanitizers

```makefile
# Makefile
CC = gcc
CFLAGS = -Wall -Wextra -std=c11 -fsanitize=address,undefined
LDFLAGS = -fsanitize=address,undefined

test: tests/test_lista tests/test_fila
	./tests/test_lista && ./tests/test_fila

tests/test_lista: src/u1/lista.c tests/test_lista.c
	$(CC) $(CFLAGS) -Iinclude $^ -o $@ $(LDFLAGS)
```

## GitHub Actions CI

```yaml
# .github/workflows/ci.yml
name: CI

on: [push, pull_request]

jobs:
  test-c:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Compilar e testar C
        run: |
          cd aed-c
          make test

  test-java:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: 'maven'
      - name: Testar Java
        run: ./mvnw test

  build-docker:
    needs: test-java
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Build Docker image
        run: docker build -t rota-vital .
```

## Critérios de Aceite

### AED U1
- Lista/Fila em C e Java com malloc/free explícitos
- Inserir, remover, consultar testados nas duas linguagens
- Mesmas fixtures → mesmos resultados C e Java
- Tradução comentada citando cabeça, cauda, próximo nó, liberação

### SO U1
- Threads preservadas: endpoint, 2/4/8 threads, tabela, gráfico, speedup
- CI verde, URL pública, health check respondendo

## Riscos Conhecidos

| Risco | Mitigação |
|-------|-----------|
| Maven Wrapper falhando no Windows | Baixar `mvnw` e `maven-wrapper.properties` oficiais |
| Perder evidência de threads | Nunca reescrever; apenas integrar e corrigir reprodutibilidade |
| Controllers sem lógica real | Controllers só chamam serviços; serviços usam estruturas AED |
| Banco de dados desnecessário | Dados sintéticos em memória na U1 |
