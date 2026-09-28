#ifndef FILA_H
#define FILA_H

#include "requisicao.h"

/**
 * Nó da fila encadeada de requisições.
 *
 * Equivalente Java: NoRequisicao (aed/u1/NoRequisicao.java)
 */
typedef struct NoFila {
    Requisicao requisicao;     /* dado armazenado */
    struct NoFila *proximo;    /* ponteiro para o próximo nó */
} NoFila;

/**
 * Fila simplesmente encadeada de requisições emergenciais (FIFO).
 *
 * Equivalente Java: FilaRequisicoes (aed/u1/FilaRequisicoes.java)
 * - cabeca → FilaRequisicoes.cabeca  (onde se desenfileira)
 * - cauda  → FilaRequisicoes.cauda   (onde se enfileira)
 * - tamanho → FilaRequisicoes.tamanho
 *
 * Dois ponteiros garantem O(1) para enfileirar e desenfileirar.
 */
typedef struct {
    NoFila *cabeca;   /* primeiro nó — dequeue aqui */
    NoFila *cauda;    /* último nó — enqueue aqui */
    int tamanho;
} Fila;

/**
 * Cria uma fila vazia.
 * Equivalente Java: new FilaRequisicoes()
 */
Fila* fila_criar(void);

/**
 * Enfileira uma requisição no final da fila (FIFO).
 * Equivalente Java: FilaRequisicoes.enfileirar(Requisicao)
 *
 * @param f ponteiro para a fila
 * @param r requisição a enfileirar (copiada para dentro do nó)
 */
void fila_enfileirar(Fila *f, Requisicao r);

/**
 * Desenfileira a requisição do início da fila (FIFO).
 * Equivalente Java: FilaRequisicoes.desenfileirar()
 *
 * @param f ponteiro para a fila
 * @param out ponteiro onde o dado será copiado (se fila não vazia)
 * @return 1 se conseguiu desenfileirar, 0 se fila vazia
 */
int fila_desenfileirar(Fila *f, Requisicao *out);

/**
 * Libera toda a memória da fila (todos os nós + struct Fila).
 * Equivalente Java: garbage collector
 */
void fila_liberar(Fila *f);

#endif /* FILA_H */
