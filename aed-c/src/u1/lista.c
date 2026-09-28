/**
 * lista.c — Implementação da lista simplesmente encadeada de bolsas.
 *
 * Cada operação é comentada com a equivalência Java (ListaEstoque.java).
 * Em C, toda alocação usa malloc() e deve ser liberada com free().
 *
 * Compilar com: gcc -Wall -Wextra -std=c11 -fsanitize=address,undefined
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "../include/lista.h"

/**
 * Cria uma lista vazia.
 * Java: new ListaEstoque()  →  cabeca = null; tamanho = 0;
 */
Lista* lista_criar(void) {
    Lista *l = malloc(sizeof(Lista));
    if (l == NULL) {
        fprintf(stderr, "Erro: malloc falhou em lista_criar\n");
        exit(EXIT_FAILURE);
    }
    l->cabeca = NULL;    /* Java: this.cabeca = null; */
    l->tamanho = 0;      /* Java: this.tamanho = 0;   */
    return l;
}

/**
 * Insere uma bolsa no início da lista.
 *
 * Java equivalente (ListaEstoque.inserir):
 *   NoBolsa novo = new NoBolsa(b);   // aqui: malloc(sizeof(No))
 *   novo.proximo = cabeca;           // novo->proximo = l->cabeca
 *   cabeca = novo;                   // l->cabeca = novo
 *   tamanho++;                       // l->tamanho++
 *
 * Complexidade: O(1)
 */
void lista_inserir(Lista *l, Bolsa b) {
    No *novo = malloc(sizeof(No));    /* Java: NoBolsa novo = new NoBolsa(b); */
    if (novo == NULL) {
        fprintf(stderr, "Erro: malloc falhou em lista_inserir\n");
        exit(EXIT_FAILURE);
    }
    novo->bolsa = b;                  /* copia a struct inteira */
    novo->proximo = l->cabeca;        /* Java: novo.proximo = cabeca; */
    l->cabeca = novo;                 /* Java: cabeca = novo; */
    l->tamanho++;                     /* Java: tamanho++; */
}

/**
 * Consulta uma bolsa pelo ID, percorrendo a lista.
 *
 * Java equivalente (ListaEstoque.consultar):
 *   NoBolsa atual = cabeca;
 *   while (atual != null) {
 *       if (atual.dado.getId().equals(id)) return Optional.of(atual.dado);
 *       atual = atual.proximo;
 *   }
 *   return Optional.empty();
 *
 * Complexidade: O(n)
 *
 * @return ponteiro para a Bolsa dentro do nó, ou NULL
 */
Bolsa* lista_consultar(Lista *l, const char *id) {
    No *atual = l->cabeca;            /* Java: NoBolsa atual = cabeca; */
    while (atual != NULL) {           /* Java: while (atual != null)   */
        if (strcmp(atual->bolsa.id, id) == 0) {  /* Java: .equals(id) */
            return &(atual->bolsa);   /* Java: return Optional.of(atual.dado); */
        }
        atual = atual->proximo;       /* Java: atual = atual.proximo; */
    }
    return NULL;                      /* Java: return Optional.empty(); */
}

/**
 * Remove uma bolsa pelo ID.
 *
 * Java equivalente (ListaEstoque.remover):
 *   - Caso cabeça: cabeca = cabeca.proximo; (GC libera)
 *   - Caso geral: anterior.proximo = anterior.proximo.proximo; (GC libera)
 *
 * Em C, o nó removido DEVE ser liberado com free().
 *
 * Complexidade: O(n)
 */
int lista_remover(Lista *l, const char *id) {
    if (l->cabeca == NULL) {
        return 0;                     /* lista vazia */
    }

    /* Caso especial: remoção da cabeça */
    if (strcmp(l->cabeca->bolsa.id, id) == 0) {
        No *temp = l->cabeca;         /* guarda referência para free */
        l->cabeca = l->cabeca->proximo; /* Java: cabeca = cabeca.proximo; */
        free(temp);                   /* Java: garbage collector faria isso */
        l->tamanho--;
        return 1;
    }

    /* Caso geral: percorre até encontrar o anterior ao nó alvo */
    No *anterior = l->cabeca;
    while (anterior->proximo != NULL) {
        if (strcmp(anterior->proximo->bolsa.id, id) == 0) {
            No *removido = anterior->proximo;
            anterior->proximo = removido->proximo;  /* religa ponteiros */
            free(removido);           /* libera memória do nó removido */
            l->tamanho--;
            return 1;
        }
        anterior = anterior->proximo;
    }

    return 0;                         /* não encontrou */
}

/**
 * Libera toda a memória da lista.
 * Em Java, isso seria feito automaticamente pelo garbage collector.
 * Em C, é OBRIGATÓRIO chamar para evitar vazamentos.
 *
 * Percorre cada nó, salva o próximo, libera o atual, avança.
 */
void lista_liberar(Lista *l) {
    No *atual = l->cabeca;
    while (atual != NULL) {
        No *proximo = atual->proximo; /* salva referência antes de liberar */
        free(atual);                  /* libera o nó atual */
        atual = proximo;              /* avança para o próximo */
    }
    free(l);                          /* libera a struct Lista */
}
