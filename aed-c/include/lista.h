#ifndef LISTA_H
#define LISTA_H

#include "bolsa.h"

/**
 * Nó da lista simplesmente encadeada de bolsas.
 *
 * Equivalente Java: NoBolsa (aed/u1/NoBolsa.java)
 * - dado   → NoBolsa.dado
 * - proximo → NoBolsa.proximo (referência em Java, ponteiro em C)
 *
 * Em C, cada nó é alocado com malloc(sizeof(No)) e liberado com free().
 * Em Java, o garbage collector cuida da desalocação.
 */
typedef struct No {
    Bolsa bolsa;          /* dado armazenado */
    struct No *proximo;   /* ponteiro para o próximo nó (NULL = fim) */
} No;

/**
 * Lista simplesmente encadeada de bolsas (estoque).
 *
 * Equivalente Java: ListaEstoque (aed/u1/ListaEstoque.java)
 * - cabeca → ListaEstoque.cabeca
 * - tamanho → ListaEstoque.tamanho
 */
typedef struct {
    No *cabeca;   /* ponteiro para o primeiro nó (NULL = lista vazia) */
    int tamanho;  /* quantidade de elementos */
} Lista;

/**
 * Cria uma lista vazia.
 * Equivalente Java: new ListaEstoque()
 *
 * @return ponteiro para a lista (alocada com malloc)
 */
Lista* lista_criar(void);

/**
 * Insere uma bolsa no início da lista.
 * Equivalente Java: ListaEstoque.inserir(Bolsa)
 *
 * @param l ponteiro para a lista
 * @param b bolsa a inserir (copiada para dentro do nó)
 */
void lista_inserir(Lista *l, Bolsa b);

/**
 * Consulta uma bolsa pelo ID.
 * Equivalente Java: ListaEstoque.consultar(String id)
 *
 * @param l ponteiro para a lista
 * @param id identificador da bolsa
 * @return ponteiro para a Bolsa encontrada, ou NULL se não existir
 */
Bolsa* lista_consultar(Lista *l, const char *id);

/**
 * Remove uma bolsa pelo ID.
 * Equivalente Java: ListaEstoque.remover(String id)
 *
 * @param l ponteiro para a lista
 * @param id identificador da bolsa a remover
 * @return 1 se removeu, 0 se não encontrou
 */
int lista_remover(Lista *l, const char *id);

/**
 * Libera toda a memória da lista (todos os nós + struct Lista).
 * Equivalente Java: garbage collector (sem equivalente direto)
 *
 * Em C, DEVE ser chamada para evitar vazamento de memória.
 *
 * @param l ponteiro para a lista a liberar
 */
void lista_liberar(Lista *l);

#endif /* LISTA_H */
