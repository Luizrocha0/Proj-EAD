#include "aed_u2.h"

#include <assert.h>
#include <limits.h>
#include <math.h>
#include <stdio.h>
#include <string.h>

static void testar_matriz_e_indice(void) {
    const int mascaras[] = {1, 3, 5, 15, 17, 51, 85, 255};
    for (int receptor = 0; receptor < TOTAL_TIPOS; receptor++) {
        for (int doador = 0; doador < TOTAL_TIPOS; doador++) {
            assert(compativel_abo_rh((TipoSanguineo)doador, (TipoSanguineo)receptor)
                   == ((mascaras[receptor] & (1 << doador)) != 0));
        }
    }
    assert(!compativel_abo_rh(TIPO_INVALIDO, A_NEG));
    assert(tipo_sanguineo("AB+") == AB_POS);
    assert(tipo_sanguineo("X?") == TIPO_INVALIDO);

    IndiceEstoque *indice = indice_estoque_criar();
    assert(indice != NULL);
    assert(indice_estoque_inserir(indice, O_NEG, 0));
    assert(indice_estoque_inserir(indice, B_POS, 1));
    assert(indice_estoque_inserir(indice, O_NEG, 3));
    size_t total;
    const size_t *posicoes = indice_estoque_buscar(indice, O_NEG, &total);
    assert(total == 2 && posicoes[0] == 0 && posicoes[1] == 3);
    posicoes = indice_estoque_buscar(indice, B_POS, &total);
    assert(total == 1 && posicoes[0] == 1);
    assert(indice_estoque_buscar(indice, AB_NEG, &total) == NULL && total == 0);
    indice_estoque_liberar(indice);
}

static void testar_atendimento_e_fefo(void) {
    const Bolsa estoque[] = {
        {.id = "DUP", .tipo_sanguineo = "A-", .validade = "2026-11-28"},
        {.id = "VENCIDA", .tipo_sanguineo = "O-", .validade = "2026-11-22"},
        {.id = "EMPATE-RECENTE", .tipo_sanguineo = "A-", .validade = "2026-11-25"},
        {.id = "DUP", .tipo_sanguineo = "O-", .validade = "2026-11-23"},
        {.id = "INCOMPATIVEL", .tipo_sanguineo = "A+", .validade = "2026-11-23"},
        {.id = "EMPATE-ANTIGO", .tipo_sanguineo = "O-", .validade = "2026-11-25"},
        {.id = "TIPO-LEGADO", .tipo_sanguineo = "X?", .validade = "2026-11-23"}
    };
    const PontoRota origem = {"CENTRO", 0, 0};
    const PontoRota paradas[] = {{"A", 0, 4}, {"B", 2, 0}, {"C", 5, 0}};
    const PontoRota destino = {"HOSP", 8, 1};
    Requisicao pedido = {.id = "REQ", .hospital_id = "HOSP", .tipo_sanguineo = "A-", .quantidade_bolsas = 4};
    size_t selecionadas[5];
    PontoRota trajeto[5];
    double km;

    assert(atender_requisicao(estoque, 7, &pedido, "2026-11-23", origem, paradas, 3, destino,
                              selecionadas, trajeto, &km) == ALOCACAO_OK);
    assert(selecionadas[0] == 3 && selecionadas[1] == 2 && selecionadas[2] == 5 && selecionadas[3] == 0);
    assert(strcmp(trajeto[0].id, "CENTRO") == 0);
    assert(strcmp(trajeto[1].id, "B") == 0);
    assert(strcmp(trajeto[2].id, "C") == 0);
    assert(strcmp(trajeto[3].id, "A") == 0);
    assert(strcmp(trajeto[4].id, "HOSP") == 0);
    assert(fabs(km - (5 + sqrt(41) + sqrt(73))) < 1e-10);

    pedido.quantidade_bolsas = 5;
    selecionadas[0] = 100;
    assert(atender_requisicao(estoque, 7, &pedido, "2026-11-23", origem, paradas, 3, destino,
                              selecionadas, trajeto, &km) == ESTOQUE_INSUFICIENTE);
    assert(selecionadas[0] == 100);
    assert(alocar_fefo(NULL, 0, A_NEG, 1, "2026-11-23", selecionadas) == ESTOQUE_INSUFICIENTE);

    const PontoRota empatadas[] = {{"ESQUERDA", -1, 0}, {"DIREITA", 1, 0}};
    assert(rota_vizinho_mais_proximo(origem, empatadas, 2, destino, trajeto, &km));
    assert(strcmp(trajeto[1].id, "ESQUERDA") == 0);
    const PontoRota direto = {"HOSP", 3, 4};
    assert(rota_vizinho_mais_proximo(origem, NULL, 0, direto, trajeto, &km));
    assert(km == 5.0);
    assert(!rota_vizinho_mais_proximo(origem, NULL, 0, origem, trajeto, &km));
    const PontoRota invalido = {"NAN", NAN, 0};
    assert(!rota_vizinho_mais_proximo(origem, NULL, 0, invalido, trajeto, &km));
}

static void testar_buscas_e_ordenacoes(void) {
    const int ordem[] = {-4, 2, 2, 2, 9};
    const int desordem[] = {9, 4, 4, -1};
    assert(busca_sequencial(desordem, 4, 4) == 1);
    assert(busca_binaria(ordem, 5, 2) == 1);
    assert(busca_binaria(ordem, 5, 9) == 4);
    assert(busca_binaria(ordem, 5, 5) == -1);
    assert(busca_sequencial(NULL, 0, 4) == -1);
    assert(busca_binaria(NULL, 0, 4) == -1);

    void (*ordenar[])(int *, size_t) = {bubble_sort, selection_sort, insertion_sort, quick_sort, heap_sort};
    const int valores[] = {4, -2, 4, 0, INT_MIN, INT_MAX, 1, -2, 19};
    const int esperado[] = {INT_MIN, -2, -2, 0, 1, 4, 4, 19, INT_MAX};
    const size_t total = sizeof(valores) / sizeof(valores[0]);
    int atual[sizeof(valores) / sizeof(valores[0])];
    for (size_t i = 0; i < sizeof(ordenar) / sizeof(ordenar[0]); i++) {
        ordenar[i](NULL, 0);
        memcpy(atual, valores, sizeof(valores));
        ordenar[i](atual, total);
        assert(memcmp(atual, esperado, sizeof(esperado)) == 0);
    }
    assert(merge_sort(NULL, 0));
    memcpy(atual, valores, sizeof(valores));
    assert(merge_sort(atual, total));
    assert(memcmp(atual, esperado, sizeof(esperado)) == 0);

    int iguais[512] = {0};
    quick_sort(iguais, 512);
    for (size_t i = 0; i < 512; i++) {
        assert(iguais[i] == 0);
    }
}

int main(void) {
    testar_matriz_e_indice();
    testar_atendimento_e_fefo();
    testar_buscas_e_ordenacoes();
    puts("AED U2: matriz, hash, FEFO, rota, buscas e ordenacoes aprovados.");
    return 0;
}
