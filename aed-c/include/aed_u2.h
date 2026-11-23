#ifndef AED_U2_H
#define AED_U2_H

#include "bolsa.h"
#include "requisicao.h"

#include <stdbool.h>
#include <stddef.h>

typedef enum {
    TIPO_INVALIDO = -1,
    O_NEG, O_POS, A_NEG, A_POS, B_NEG, B_POS, AB_NEG, AB_POS,
    TOTAL_TIPOS
} TipoSanguineo;

typedef struct IndiceEstoque IndiceEstoque;

typedef struct {
    const char *id;
    double x_km;
    double y_km;
} PontoRota;

typedef enum {
    ALOCACAO_OK,
    ESTOQUE_INSUFICIENTE,
    ERRO_ALOCACAO
} ResultadoAlocacao;

TipoSanguineo tipo_sanguineo(const char *rotulo);
bool compativel_abo_rh(TipoSanguineo doador, TipoSanguineo receptor);

IndiceEstoque *indice_estoque_criar(void);
bool indice_estoque_inserir(IndiceEstoque *indice, TipoSanguineo tipo, size_t posicao);
const size_t *indice_estoque_buscar(const IndiceEstoque *indice, TipoSanguineo tipo, size_t *quantidade);
void indice_estoque_liberar(IndiceEstoque *indice);

/* Estoque em LIFO e datas ISO AAAA-MM-DD. O chamador reserva 'quantidade' posições. */
ResultadoAlocacao alocar_fefo(const Bolsa *estoque, size_t total, TipoSanguineo receptor,
                             size_t quantidade, const char *referencia, size_t *selecionadas);

/* O chamador reserva 'total_paradas + 2' pontos. Os IDs devem estar normalizados. */
bool rota_vizinho_mais_proximo(PontoRota origem, const PontoRota *paradas, size_t total_paradas,
                               PontoRota destino, PontoRota *trajeto, double *distancia_km);

/* Seleção e trajeto integram o resultado quando o retorno é ALOCACAO_OK. */
ResultadoAlocacao atender_requisicao(const Bolsa *estoque, size_t total, const Requisicao *pedido,
                                    const char *referencia, PontoRota origem, const PontoRota *paradas,
                                    size_t total_paradas, PontoRota destino, size_t *selecionadas,
                                    PontoRota *trajeto, double *distancia_km);

ptrdiff_t busca_sequencial(const int *valores, size_t tamanho, int alvo);
ptrdiff_t busca_binaria(const int *valores_ordenados, size_t tamanho, int alvo);

void bubble_sort(int *valores, size_t tamanho);
void selection_sort(int *valores, size_t tamanho);
void insertion_sort(int *valores, size_t tamanho);
bool merge_sort(int *valores, size_t tamanho);
void quick_sort(int *valores, size_t tamanho);
void heap_sort(int *valores, size_t tamanho);

#endif
