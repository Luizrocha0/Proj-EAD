#include "aed_u2.h"

#include <stdint.h>
#include <stdlib.h>
#include <string.h>

enum { TOTAL_BALDES = 5 };

typedef struct Entrada {
    TipoSanguineo tipo;
    size_t *posicoes;
    size_t tamanho;
    size_t capacidade;
    struct Entrada *proxima;
} Entrada;

struct IndiceEstoque {
    Entrada *baldes[TOTAL_BALDES];
};

static bool tipo_valido(TipoSanguineo tipo) {
    return tipo >= O_NEG && tipo < TOTAL_TIPOS;
}

TipoSanguineo tipo_sanguineo(const char *rotulo) {
    const char *const rotulos[] = {"O-", "O+", "A-", "A+", "B-", "B+", "AB-", "AB+"};
    if (rotulo != NULL) {
        for (int i = 0; i < TOTAL_TIPOS; i++) {
            if (strcmp(rotulos[i], rotulo) == 0) {
                return (TipoSanguineo)i;
            }
        }
    }
    return TIPO_INVALIDO;
}

bool compativel_abo_rh(TipoSanguineo doador, TipoSanguineo receptor) {
    /* Java usa os mesmos índices do enum: a linha recebe, a coluna doa. */
    static const bool matriz[TOTAL_TIPOS][TOTAL_TIPOS] = {
        {true,  false, false, false, false, false, false, false}, /* O- */
        {true,  true,  false, false, false, false, false, false}, /* O+ */
        {true,  false, true,  false, false, false, false, false}, /* A- */
        {true,  true,  true,  true,  false, false, false, false}, /* A+ */
        {true,  false, false, false, true,  false, false, false}, /* B- */
        {true,  true,  false, false, true,  true,  false, false}, /* B+ */
        {true,  false, true,  false, true,  false, true,  false}, /* AB- */
        {true,  true,  true,  true,  true,  true,  true,  true}   /* AB+ */
    };
    return tipo_valido(doador) && tipo_valido(receptor) && matriz[receptor][doador];
}

IndiceEstoque *indice_estoque_criar(void) {
    IndiceEstoque *indice = malloc(sizeof(*indice));
    if (indice != NULL) {
        *indice = (IndiceEstoque){0};
    }
    return indice;
}

static size_t balde(TipoSanguineo tipo) {
    return (size_t)tipo % TOTAL_BALDES;
}

static Entrada *encontrar(const IndiceEstoque *indice, TipoSanguineo tipo) {
    for (Entrada *atual = indice->baldes[balde(tipo)]; atual != NULL; atual = atual->proxima) {
        if (atual->tipo == tipo) {
            return atual;
        }
    }
    return NULL;
}

bool indice_estoque_inserir(IndiceEstoque *indice, TipoSanguineo tipo, size_t posicao) {
    if (indice == NULL || !tipo_valido(tipo)) {
        return false;
    }
    Entrada *entrada = encontrar(indice, tipo);
    if (entrada == NULL) {
        entrada = malloc(sizeof(*entrada));
        if (entrada == NULL) {
            return false;
        }
        size_t chave = balde(tipo);
        *entrada = (Entrada){.tipo = tipo, .proxima = indice->baldes[chave]};
        indice->baldes[chave] = entrada;
    }

    /* realloc amplia o vetor de posições. No Java, ArrayList gerencia essa capacidade. */
    if (entrada->tamanho == entrada->capacidade) {
        if (entrada->capacidade > SIZE_MAX / sizeof(size_t) / 2) {
            return false;
        }
        size_t nova_capacidade = entrada->capacidade == 0 ? 4 : entrada->capacidade * 2;
        size_t *novas_posicoes = realloc(entrada->posicoes, nova_capacidade * sizeof(*novas_posicoes));
        if (novas_posicoes == NULL) {
            return false;
        }
        entrada->posicoes = novas_posicoes;
        entrada->capacidade = nova_capacidade;
    }
    entrada->posicoes[entrada->tamanho++] = posicao;
    return true;
}

const size_t *indice_estoque_buscar(const IndiceEstoque *indice, TipoSanguineo tipo, size_t *quantidade) {
    Entrada *entrada = indice != NULL && tipo_valido(tipo) ? encontrar(indice, tipo) : NULL;
    *quantidade = entrada == NULL ? 0 : entrada->tamanho;
    return entrada == NULL ? NULL : entrada->posicoes;
}

void indice_estoque_liberar(IndiceEstoque *indice) {
    if (indice == NULL) {
        return;
    }
    for (size_t i = 0; i < TOTAL_BALDES; i++) {
        Entrada *entrada = indice->baldes[i];
        while (entrada != NULL) {
            Entrada *proxima = entrada->proxima;
            free(entrada->posicoes);
            free(entrada);
            entrada = proxima;
        }
    }
    free(indice);
}

static bool precede(const Bolsa *estoque, size_t primeira, size_t segunda) {
    /* O formato ISO permite a mesma ordem cronológica de LocalDate.compareTo. */
    int comparacao = strcmp(estoque[primeira].validade, estoque[segunda].validade);
    return comparacao < 0 || (comparacao == 0 && primeira < segunda);
}

static void inserir_ordenado(const Bolsa *estoque, size_t *ordenadas, size_t *usadas,
                             size_t candidata, size_t quantidade) {
    size_t inicio = 0;
    size_t fim = *usadas;
    while (inicio < fim) {
        size_t meio = inicio + (fim - inicio) / 2;
        if (precede(estoque, ordenadas[meio], candidata)) {
            inicio = meio + 1;
        } else {
            fim = meio;
        }
    }

    if (inicio == quantidade) {
        return;
    }
    if (*usadas == quantidade) {
        (*usadas)--;
    }
    /* memmove permite sobreposição ao abrir espaço, como ArrayList.add(posicao, valor). */
    memmove(ordenadas + inicio + 1, ordenadas + inicio, (*usadas - inicio) * sizeof(*ordenadas));
    ordenadas[inicio] = candidata;
    (*usadas)++;
}

ResultadoAlocacao alocar_fefo(const Bolsa *estoque, size_t total, TipoSanguineo receptor,
                             size_t quantidade, const char *referencia, size_t *selecionadas) {
    if (!tipo_valido(receptor) || quantidade == 0 || referencia == NULL || selecionadas == NULL
        || (total > 0 && estoque == NULL) || quantidade > SIZE_MAX / sizeof(size_t)) {
        return ERRO_ALOCACAO;
    }
    if (quantidade > total) {
        return ESTOQUE_INSUFICIENTE;
    }

    IndiceEstoque *indice = indice_estoque_criar();
    if (indice == NULL) {
        return ERRO_ALOCACAO;
    }
    for (size_t i = 0; i < total; i++) {
        if (strcmp(estoque[i].validade, referencia) >= 0) {
            TipoSanguineo tipo = tipo_sanguineo(estoque[i].tipo_sanguineo);
            if (tipo_valido(tipo) && !indice_estoque_inserir(indice, tipo, i)) {
                indice_estoque_liberar(indice);
                return ERRO_ALOCACAO;
            }
        }
    }

    size_t *ordenadas = malloc(quantidade * sizeof(*ordenadas));
    if (ordenadas == NULL) {
        indice_estoque_liberar(indice);
        return ERRO_ALOCACAO;
    }
    size_t usadas = 0;
    for (int doador = 0; doador < TOTAL_TIPOS; doador++) {
        if (compativel_abo_rh((TipoSanguineo)doador, receptor)) {
            size_t tamanho;
            const size_t *posicoes = indice_estoque_buscar(indice, (TipoSanguineo)doador, &tamanho);
            for (size_t i = 0; i < tamanho; i++) {
                inserir_ordenado(estoque, ordenadas, &usadas, posicoes[i], quantidade);
            }
        }
    }

    if (usadas == quantidade) {
        memcpy(selecionadas, ordenadas, quantidade * sizeof(*selecionadas));
    }
    free(ordenadas);
    indice_estoque_liberar(indice);
    return usadas == quantidade ? ALOCACAO_OK : ESTOQUE_INSUFICIENTE;
}
