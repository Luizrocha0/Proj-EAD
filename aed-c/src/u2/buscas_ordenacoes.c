#include "aed_u2.h"

#include <stdint.h>
#include <stdlib.h>
#include <string.h>

ptrdiff_t busca_sequencial(const int *valores, size_t tamanho, int alvo) {
    for (size_t i = 0; i < tamanho; i++) {
        if (valores[i] == alvo) {
            return (ptrdiff_t)i;
        }
    }
    return -1;
}

ptrdiff_t busca_binaria(const int *valores_ordenados, size_t tamanho, int alvo) {
    /* Mesmo limite inferior semiaberto de Buscas.binaria e da inserção FEFO. */
    size_t inicio = 0;
    size_t fim = tamanho;
    while (inicio < fim) {
        size_t meio = inicio + (fim - inicio) / 2;
        if (valores_ordenados[meio] < alvo) {
            inicio = meio + 1;
        } else {
            fim = meio;
        }
    }
    return inicio < tamanho && valores_ordenados[inicio] == alvo ? (ptrdiff_t)inicio : -1;
}

static void trocar(int *valores, size_t primeira, size_t segunda) {
    int temporario = valores[primeira];
    valores[primeira] = valores[segunda];
    valores[segunda] = temporario;
}

void bubble_sort(int *valores, size_t tamanho) {
    for (size_t fim = tamanho; fim > 1; fim--) {
        bool trocou = false;
        for (size_t i = 1; i < fim; i++) {
            if (valores[i - 1] > valores[i]) {
                trocar(valores, i - 1, i);
                trocou = true;
            }
        }
        if (!trocou) {
            return;
        }
    }
}

void selection_sort(int *valores, size_t tamanho) {
    for (size_t i = 1; i < tamanho; i++) {
        size_t menor = i - 1;
        for (size_t j = i; j < tamanho; j++) {
            if (valores[j] < valores[menor]) {
                menor = j;
            }
        }
        if (menor != i - 1) {
            trocar(valores, i - 1, menor);
        }
    }
}

void insertion_sort(int *valores, size_t tamanho) {
    for (size_t i = 1; i < tamanho; i++) {
        int chave = valores[i];
        size_t posicao = i;
        while (posicao > 0 && valores[posicao - 1] > chave) {
            valores[posicao] = valores[posicao - 1];
            posicao--;
        }
        valores[posicao] = chave;
    }
}

static void intercalar(int *valores, int *auxiliar, size_t inicio, size_t fim) {
    if (fim - inicio < 2) {
        return;
    }
    size_t meio = inicio + (fim - inicio) / 2;
    intercalar(valores, auxiliar, inicio, meio);
    intercalar(valores, auxiliar, meio, fim);

    size_t esquerda = inicio;
    size_t direita = meio;
    for (size_t i = inicio; i < fim; i++) {
        if (direita == fim || (esquerda < meio && valores[esquerda] <= valores[direita])) {
            auxiliar[i] = valores[esquerda++];
        } else {
            auxiliar[i] = valores[direita++];
        }
    }
    memcpy(valores + inicio, auxiliar + inicio, (fim - inicio) * sizeof(*valores));
}

bool merge_sort(int *valores, size_t tamanho) {
    if (tamanho < 2) {
        return true;
    }
    if (tamanho > SIZE_MAX / sizeof(int)) {
        return false;
    }
    int *auxiliar = malloc(tamanho * sizeof(*auxiliar));
    if (auxiliar == NULL) {
        return false;
    }
    intercalar(valores, auxiliar, 0, tamanho);
    free(auxiliar);
    return true;
}

static void particionar(int *valores, size_t inicio, size_t fim) {
    while (fim - inicio > 1) {
        int pivo = valores[inicio + (fim - inicio) / 2];
        size_t menores = inicio;
        size_t maiores = fim;
        size_t atual = inicio;
        while (atual < maiores) {
            if (valores[atual] < pivo) {
                trocar(valores, menores, atual);
                menores++;
                atual++;
            } else if (valores[atual] > pivo) {
                maiores--;
                trocar(valores, atual, maiores);
            } else {
                atual++;
            }
        }

        /* A menor partição consome pilha. A maior reutiliza este quadro, como em Java. */
        if (menores - inicio < fim - maiores) {
            particionar(valores, inicio, menores);
            inicio = maiores;
        } else {
            particionar(valores, maiores, fim);
            fim = menores;
        }
    }
}

void quick_sort(int *valores, size_t tamanho) {
    particionar(valores, 0, tamanho);
}

static void descer_no_heap(int *valores, size_t raiz, size_t tamanho) {
    while (raiz < tamanho / 2) {
        size_t filho = raiz * 2 + 1;
        size_t direito = filho + 1;
        if (direito < tamanho && valores[direito] > valores[filho]) {
            filho = direito;
        }
        if (valores[raiz] >= valores[filho]) {
            return;
        }
        trocar(valores, raiz, filho);
        raiz = filho;
    }
}

void heap_sort(int *valores, size_t tamanho) {
    for (size_t pai = tamanho / 2; pai > 0; pai--) {
        descer_no_heap(valores, pai - 1, tamanho);
    }
    for (size_t fim = tamanho; fim > 1; fim--) {
        trocar(valores, 0, fim - 1);
        descer_no_heap(valores, 0, fim - 1);
    }
}
