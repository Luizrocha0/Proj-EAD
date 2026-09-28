/**
 * fila.c — Implementação da fila simplesmente encadeada de requisições (FIFO).
 *
 * Cada operação é comentada com a equivalência Java (FilaRequisicoes.java).
 * Em C, toda alocação usa malloc() e deve ser liberada com free().
 *
 * Compilar com: gcc -Wall -Wextra -std=c11 -fsanitize=address,undefined
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "../include/fila.h"

/**
 * Cria uma fila vazia.
 * Java: new FilaRequisicoes()  →  cabeca = null; cauda = null; tamanho = 0;
 */
Fila* fila_criar(void) {
    Fila *f = malloc(sizeof(Fila));
    if (f == NULL) {
        fprintf(stderr, "Erro: malloc falhou em fila_criar\n");
        exit(EXIT_FAILURE);
    }
    f->cabeca = NULL;     /* Java: this.cabeca = null; */
    f->cauda = NULL;      /* Java: this.cauda = null;  */
    f->tamanho = 0;       /* Java: this.tamanho = 0;   */
    return f;
}

/**
 * Enfileira uma requisição no final da fila (FIFO).
 *
 * Java equivalente (FilaRequisicoes.enfileirar):
 *   NoRequisicao novo = new NoRequisicao(r);
 *   if (cauda == null) { cabeca = novo; cauda = novo; }
 *   else { cauda.proximo = novo; cauda = novo; }
 *   tamanho++;
 *
 * Complexidade: O(1) — graças ao ponteiro cauda
 */
void fila_enfileirar(Fila *f, Requisicao r) {
    NoFila *novo = malloc(sizeof(NoFila));  /* Java: new NoRequisicao(r); */
    if (novo == NULL) {
        fprintf(stderr, "Erro: malloc falhou em fila_enfileirar\n");
        exit(EXIT_FAILURE);
    }
    novo->requisicao = r;                  /* copia a struct */
    novo->proximo = NULL;

    if (f->cauda == NULL) {
        /* Fila vazia: cabeca e cauda apontam para o mesmo nó */
        /* Java: cabeca = novo; cauda = novo; */
        f->cabeca = novo;
        f->cauda = novo;
    } else {
        /* Insere no final */
        /* Java: cauda.proximo = novo; cauda = novo; */
        f->cauda->proximo = novo;
        f->cauda = novo;
    }
    f->tamanho++;
}

/**
 * Desenfileira a requisição do início da fila (FIFO).
 *
 * Java equivalente (FilaRequisicoes.desenfileirar):
 *   Requisicao dado = cabeca.dado;
 *   cabeca = cabeca.proximo;
 *   if (cabeca == null) cauda = null;
 *   tamanho--;
 *   return Optional.of(dado);
 *
 * Em C, o nó removido DEVE ser liberado com free().
 *
 * Complexidade: O(1)
 *
 * @return 1 se conseguiu (out preenchido), 0 se fila vazia
 */
int fila_desenfileirar(Fila *f, Requisicao *out) {
    if (f->cabeca == NULL) {
        return 0;                          /* Java: return Optional.empty(); */
    }

    *out = f->cabeca->requisicao;          /* Java: Requisicao dado = cabeca.dado; */

    NoFila *temp = f->cabeca;              /* guarda para free */
    f->cabeca = f->cabeca->proximo;        /* Java: cabeca = cabeca.proximo; */

    if (f->cabeca == NULL) {
        /* Fila ficou vazia */
        /* Java: if (cabeca == null) cauda = null; */
        f->cauda = NULL;
    }

    free(temp);                            /* Java: garbage collector */
    f->tamanho--;
    return 1;
}

/**
 * Libera toda a memória da fila.
 * Em Java, o garbage collector faria isso automaticamente.
 * Em C, percorre cada nó e libera com free().
 */
void fila_liberar(Fila *f) {
    NoFila *atual = f->cabeca;
    while (atual != NULL) {
        NoFila *proximo = atual->proximo;  /* salva antes de liberar */
        free(atual);
        atual = proximo;
    }
    free(f);                               /* libera a struct Fila */
}
