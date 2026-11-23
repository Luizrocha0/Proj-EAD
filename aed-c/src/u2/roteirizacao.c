#include "aed_u2.h"

#include <math.h>
#include <string.h>

enum { MAX_PARADAS = 256 };

static bool ponto_valido(PontoRota ponto) {
    return ponto.id != NULL && ponto.id[0] != '\0' && strlen(ponto.id) <= 50
        && isfinite(ponto.x_km) && isfinite(ponto.y_km)
        && fabs(ponto.x_km) <= 50000 && fabs(ponto.y_km) <= 50000;
}

static bool validar_pontos(PontoRota origem, const PontoRota *paradas,
                           size_t total_paradas, PontoRota destino) {
    if (total_paradas > MAX_PARADAS || (total_paradas > 0 && paradas == NULL)
        || !ponto_valido(origem) || !ponto_valido(destino) || strcmp(origem.id, destino.id) == 0) {
        return false;
    }
    for (size_t i = 0; i < total_paradas; i++) {
        if (!ponto_valido(paradas[i]) || strcmp(paradas[i].id, origem.id) == 0
            || strcmp(paradas[i].id, destino.id) == 0) {
            return false;
        }
        for (size_t j = 0; j < i; j++) {
            if (strcmp(paradas[i].id, paradas[j].id) == 0) {
                return false;
            }
        }
    }
    return true;
}

static double distancia(PontoRota origem, PontoRota destino) {
    return hypot(origem.x_km - destino.x_km, origem.y_km - destino.y_km);
}

bool rota_vizinho_mais_proximo(PontoRota origem, const PontoRota *paradas, size_t total_paradas,
                               PontoRota destino, PontoRota *trajeto, double *distancia_km) {
    if (trajeto == NULL || distancia_km == NULL || !validar_pontos(origem, paradas, total_paradas, destino)) {
        return false;
    }

    bool visitadas[MAX_PARADAS] = {false};
    PontoRota atual = origem;
    double total_km = 0;
    trajeto[0] = origem;

    for (size_t etapa = 0; etapa < total_paradas; etapa++) {
        size_t mais_proxima = 0;
        double menor_distancia = INFINITY;
        for (size_t i = 0; i < total_paradas; i++) {
            if (!visitadas[i]) {
                double trecho = distancia(atual, paradas[i]);
                /* O mesmo '<' em Java mantém a primeira parada quando as distâncias empatam. */
                if (trecho < menor_distancia) {
                    mais_proxima = i;
                    menor_distancia = trecho;
                }
            }
        }
        visitadas[mais_proxima] = true;
        atual = paradas[mais_proxima];
        trajeto[etapa + 1] = atual;
        total_km += menor_distancia;
    }

    trajeto[total_paradas + 1] = destino;
    *distancia_km = total_km + distancia(atual, destino);
    return true;
}

ResultadoAlocacao atender_requisicao(const Bolsa *estoque, size_t total, const Requisicao *pedido,
                                    const char *referencia, PontoRota origem, const PontoRota *paradas,
                                    size_t total_paradas, PontoRota destino, size_t *selecionadas,
                                    PontoRota *trajeto, double *distancia_km) {
    if (pedido == NULL || pedido->quantidade_bolsas <= 0 || destino.id == NULL
        || strcmp(pedido->hospital_id, destino.id) != 0
        || !rota_vizinho_mais_proximo(origem, paradas, total_paradas, destino, trajeto, distancia_km)) {
        return ERRO_ALOCACAO;
    }
    return alocar_fefo(estoque, total, tipo_sanguineo(pedido->tipo_sanguineo),
                       (size_t)pedido->quantidade_bolsas, referencia, selecionadas);
}
