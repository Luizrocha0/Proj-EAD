# Equivalência U1 — Tradução Comentada C ↔ Java

**Projeto:** Rota Vital — Banco de Sangue  
**Escopo:** AED U1 — Lista Encadeada de Estoque + Fila FIFO de Requisições  
**Autores:** Eduardo Borges, Luiz Henrique Rocha, Eliziane Mota

---

## 1. Lista Encadeada de Estoque

### 1.1 Estrutura do Nó

| C (`include/lista.h`) | Java (`aed/u1/NoBolsa.java`) | Observação |
|---|---|---|
| `struct No { Bolsa bolsa; struct No *proximo; }` | `class NoBolsa { Bolsa dado; NoBolsa proximo; }` | Em C, `proximo` é um **ponteiro** (`*`) que armazena endereço de memória. Em Java, `proximo` é uma **referência** gerenciada pela JVM. O conceito é o mesmo: apontar para o próximo nó da cadeia. |

### 1.2 Estrutura da Lista

| C (`include/lista.h`) | Java (`aed/u1/ListaEstoque.java`) | Observação |
|---|---|---|
| `struct Lista { No *cabeca; int tamanho; }` | `class ListaEstoque { NoBolsa cabeca; int tamanho; }` | Ambos mantêm um ponteiro/referência para o primeiro nó (cabeça) e um contador de tamanho. |

### 1.3 Criar Lista

```c
// C — lista_criar()
Lista* lista_criar(void) {
    Lista *l = malloc(sizeof(Lista));  // aloca memória explícita
    l->cabeca = NULL;                  // sem nós
    l->tamanho = 0;
    return l;                          // retorna ponteiro
}
```

```java
// Java — construtor ListaEstoque()
public ListaEstoque() {
    this.cabeca = null;   // referência nula (equivale a NULL em C)
    this.tamanho = 0;
    // Sem malloc — a JVM aloca automaticamente com "new"
}
```

**Diferença chave:** Em C, usamos `malloc(sizeof(Lista))` para alocar memória manualmente. Em Java, o operador `new` aloca memória automaticamente na heap gerenciada pelo garbage collector.

### 1.4 Inserir no Início

```c
// C — lista_inserir()
void lista_inserir(Lista *l, Bolsa b) {
    No *novo = malloc(sizeof(No));     // aloca nó com malloc
    novo->bolsa = b;                   // copia a struct inteira
    novo->proximo = l->cabeca;         // novo aponta para o antigo primeiro
    l->cabeca = novo;                  // cabeça agora é o novo nó
    l->tamanho++;
}
```

```java
// Java — inserir()
public void inserir(Bolsa b) {
    NoBolsa novo = new NoBolsa(b);     // "new" substitui malloc
    novo.proximo = cabeca;             // mesmo encadeamento
    cabeca = novo;                     // mesma lógica
    tamanho++;
}
```

**Diferença chave:** Em C, `novo->bolsa = b` copia toda a struct byte a byte. Em Java, `novo.dado = b` copia a referência ao objeto (não o objeto em si). Em ambos os casos, a complexidade é **O(1)**.

### 1.5 Consultar por ID

```c
// C — lista_consultar()
Bolsa* lista_consultar(Lista *l, const char *id) {
    No *atual = l->cabeca;
    while (atual != NULL) {
        if (strcmp(atual->bolsa.id, id) == 0)  // compara strings com strcmp
            return &(atual->bolsa);             // retorna PONTEIRO
        atual = atual->proximo;
    }
    return NULL;                                // não encontrou
}
```

```java
// Java — consultar()
public Optional<Bolsa> consultar(String id) {
    NoBolsa atual = cabeca;
    while (atual != null) {
        if (atual.dado.getId().equals(id))      // compara com .equals()
            return Optional.of(atual.dado);     // retorna Optional (nunca null)
        atual = atual.proximo;
    }
    return Optional.empty();                    // equivale a NULL em C
}
```

**Diferenças chave:**
- C usa `strcmp()` para comparar strings; Java usa `.equals()`.
- C retorna `NULL` quando não encontra; Java retorna `Optional.empty()` (padrão mais seguro contra NullPointerException).
- Complexidade: **O(n)** em ambos.

### 1.6 Remover por ID

```c
// C — lista_remover()
int lista_remover(Lista *l, const char *id) {
    if (l->cabeca == NULL) return 0;

    // Caso cabeça
    if (strcmp(l->cabeca->bolsa.id, id) == 0) {
        No *temp = l->cabeca;
        l->cabeca = l->cabeca->proximo;
        free(temp);          // ⚠️ OBRIGATÓRIO em C — libera memória
        l->tamanho--;
        return 1;
    }

    // Caso geral
    No *anterior = l->cabeca;
    while (anterior->proximo != NULL) {
        if (strcmp(anterior->proximo->bolsa.id, id) == 0) {
            No *removido = anterior->proximo;
            anterior->proximo = removido->proximo;
            free(removido);  // ⚠️ libera o nó removido
            l->tamanho--;
            return 1;
        }
        anterior = anterior->proximo;
    }
    return 0;
}
```

```java
// Java — remover()
public boolean remover(String id) {
    if (cabeca == null) return false;

    // Caso cabeça
    if (cabeca.dado.getId().equals(id)) {
        cabeca = cabeca.proximo;  // GC libera automaticamente
        tamanho--;
        return true;
    }

    // Caso geral
    NoBolsa anterior = cabeca;
    while (anterior.proximo != null) {
        if (anterior.proximo.dado.getId().equals(id)) {
            anterior.proximo = anterior.proximo.proximo;  // GC libera
            tamanho--;
            return true;
        }
        anterior = anterior.proximo;
    }
    return false;
}
```

**Diferença chave:** Em C, **toda remoção exige `free()`** para devolver a memória ao sistema. Sem `free()`, ocorre **vazamento de memória** (memory leak). Em Java, o **garbage collector** libera automaticamente os nós que ficam sem referência. A lógica de reencadeamento dos ponteiros é idêntica.

### 1.7 Liberar Toda a Lista

```c
// C — lista_liberar() — OBRIGATÓRIO chamar
void lista_liberar(Lista *l) {
    No *atual = l->cabeca;
    while (atual != NULL) {
        No *proximo = atual->proximo;  // salva referência antes de liberar
        free(atual);                   // libera cada nó individualmente
        atual = proximo;
    }
    free(l);                           // libera a struct Lista
}
```

```java
// Java — NÃO EXISTE equivalente
// O garbage collector detecta que os nós ficaram sem referência
// e os libera automaticamente em algum momento futuro.
// A variável "lista" simplesmente sai de escopo.
```

**Diferença chave:** Esta é a diferença mais fundamental entre C e Java na gestão de memória. Em C, o programador é **100% responsável** por liberar cada byte alocado com `malloc()`. Esquecer um `free()` causa vazamento. O **AddressSanitizer** (`-fsanitize=address`) detecta esses erros em tempo de execução.

---

## 2. Fila FIFO de Requisições

### 2.1 Estrutura do Nó

| C (`include/fila.h`) | Java (`aed/u1/NoRequisicao.java`) | Observação |
|---|---|---|
| `struct NoFila { Requisicao requisicao; struct NoFila *proximo; }` | `class NoRequisicao { Requisicao dado; NoRequisicao proximo; }` | Mesmo padrão da lista, mas armazena Requisicao em vez de Bolsa. |

### 2.2 Estrutura da Fila (dois ponteiros)

| C (`include/fila.h`) | Java (`aed/u1/FilaRequisicoes.java`) | Observação |
|---|---|---|
| `struct Fila { NoFila *cabeca; NoFila *cauda; int tamanho; }` | `class FilaRequisicoes { NoRequisicao cabeca; NoRequisicao cauda; int tamanho; }` | **Dois ponteiros** garantem O(1) tanto para enfileirar (cauda) quanto para desenfileirar (cabeça). |

### 2.3 Enfileirar (no final — cauda)

```c
// C — fila_enfileirar()
void fila_enfileirar(Fila *f, Requisicao r) {
    NoFila *novo = malloc(sizeof(NoFila));
    novo->requisicao = r;
    novo->proximo = NULL;

    if (f->cauda == NULL) {
        f->cabeca = novo;   // fila estava vazia
        f->cauda = novo;
    } else {
        f->cauda->proximo = novo;  // encadeia no final
        f->cauda = novo;           // atualiza cauda
    }
    f->tamanho++;
}
```

```java
// Java — enfileirar()
public void enfileirar(Requisicao r) {
    NoRequisicao novo = new NoRequisicao(r);

    if (cauda == null) {
        cabeca = novo;      // fila estava vazia
        cauda = novo;
    } else {
        cauda.proximo = novo;  // mesmo encadeamento
        cauda = novo;
    }
    tamanho++;
}
```

**Lógica idêntica.** A diferença é `malloc` vs `new`.

### 2.4 Desenfileirar (do início — cabeça)

```c
// C — fila_desenfileirar()
int fila_desenfileirar(Fila *f, Requisicao *out) {
    if (f->cabeca == NULL) return 0;

    *out = f->cabeca->requisicao;       // copia dado para fora
    NoFila *temp = f->cabeca;
    f->cabeca = f->cabeca->proximo;

    if (f->cabeca == NULL)
        f->cauda = NULL;                // fila ficou vazia

    free(temp);                         // ⚠️ libera nó
    f->tamanho--;
    return 1;
}
```

```java
// Java — desenfileirar()
public Optional<Requisicao> desenfileirar() {
    if (cabeca == null)
        return Optional.empty();

    Requisicao dado = cabeca.dado;
    cabeca = cabeca.proximo;

    if (cabeca == null)
        cauda = null;                   // fila ficou vazia

    tamanho--;                          // GC libera o nó antigo
    return Optional.of(dado);
}
```

**Diferenças:**
- C retorna status (`0`/`1`) e copia o dado via ponteiro `out`. Java retorna `Optional<Requisicao>`.
- C exige `free()` explícito; Java delega ao garbage collector.
- **A lógica FIFO e o tratamento de cauda quando a fila esvazia são idênticos.**

---

## 3. Resumo de Complexidade

| Operação | Lista (C e Java) | Fila (C e Java) |
|---|---|---|
| Criar | O(1) | O(1) |
| Inserir/Enfileirar | O(1) | O(1) |
| Consultar | O(n) | — |
| Remover/Desenfileirar | O(n) / O(1)* | O(1) |
| Liberar | O(n) | O(n) |

\* Na lista, remover por ID é O(n) por causa da busca. Na fila, desenfileirar é sempre O(1).

---

## 4. Garantia de Equivalência

- As mesmas **fixtures** (`fixtures/bolsas_u1.json` e `fixtures/requisicoes_u1.json`) são usadas nos testes C e Java.
- Os **resultados esperados** (`fixtures/resultados_esperados_u1.json`) documentam a ordem e os valores exatos.
- Os testes C (`aed-c/tests/test_lista.c`, `test_fila.c`) e Java (`ListaEstoqueTest.java`, `FilaRequisicaoTest.java`) verificam os mesmos cenários: vazio, inserção, consulta, remoção, ordem.
