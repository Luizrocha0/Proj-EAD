/**
 * test_lista.c — Testes da lista encadeada de bolsas.
 *
 * Usa as mesmas fixtures de fixtures/bolsas_u1.json e os mesmos
 * resultados esperados de fixtures/resultados_esperados_u1.json.
 *
 * Cenários idênticos ao ListaEstoqueTest.java:
 * - Lista vazia
 * - Inserção de 5 bolsas (ordem inversa na cabeça)
 * - Consulta existente e inexistente
 * - Remoção da cabeça, meio, cauda e inexistente
 *
 * Compilar com: gcc -Wall -Wextra -std=c11 -fsanitize=address,undefined
 *               -Iinclude src/u1/lista.c tests/test_lista.c -o tests/test_lista
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <assert.h>
#include "../include/lista.h"

/* ── Contadores de testes ── */
static int testes_passaram = 0;
static int testes_total = 0;

#define ASSERT_MSG(cond, msg) do { \
    testes_total++; \
    if (!(cond)) { \
        fprintf(stderr, "  FALHOU: %s (linha %d)\n", msg, __LINE__); \
        exit(EXIT_FAILURE); \
    } \
    testes_passaram++; \
    printf("  OK: %s\n", msg); \
} while(0)

/* ── Fixtures (mesmos dados de fixtures/bolsas_u1.json) ── */
static Bolsa criar_bolsa(const char *id, const char *tipo, double vol,
                         const char *coleta, const char *val, const char *doador) {
    Bolsa b;
    strncpy(b.id, id, sizeof(b.id) - 1);
    b.id[sizeof(b.id) - 1] = '\0';
    strncpy(b.tipo_sanguineo, tipo, sizeof(b.tipo_sanguineo) - 1);
    b.tipo_sanguineo[sizeof(b.tipo_sanguineo) - 1] = '\0';
    b.volume_ml = vol;
    strncpy(b.data_coleta, coleta, sizeof(b.data_coleta) - 1);
    b.data_coleta[sizeof(b.data_coleta) - 1] = '\0';
    strncpy(b.validade, val, sizeof(b.validade) - 1);
    b.validade[sizeof(b.validade) - 1] = '\0';
    strncpy(b.doador, doador, sizeof(b.doador) - 1);
    b.doador[sizeof(b.doador) - 1] = '\0';
    return b;
}

/* ── Testes ── */

void test_lista_vazia(void) {
    printf("\n[Lista vazia]\n");
    Lista *l = lista_criar();

    ASSERT_MSG(l->tamanho == 0, "Lista recem-criada tem tamanho 0");
    ASSERT_MSG(l->cabeca == NULL, "Cabeca eh NULL em lista vazia");
    ASSERT_MSG(lista_consultar(l, "BOLSA-001") == NULL, "Consulta em lista vazia retorna NULL");
    ASSERT_MSG(lista_remover(l, "BOLSA-001") == 0, "Remocao em lista vazia retorna 0");

    lista_liberar(l);
}

void test_insercao(void) {
    printf("\n[Insercao]\n");
    Lista *l = lista_criar();

    Bolsa b1 = criar_bolsa("BOLSA-001", "O+",  450.0, "2026-09-01", "2026-11-01", "Joao Silva");
    Bolsa b2 = criar_bolsa("BOLSA-002", "A-",  450.0, "2026-09-05", "2026-11-05", "Maria Santos");
    Bolsa b3 = criar_bolsa("BOLSA-003", "B+",  400.0, "2026-09-10", "2026-11-10", "Carlos Oliveira");
    Bolsa b4 = criar_bolsa("BOLSA-004", "AB+", 450.0, "2026-08-15", "2026-10-15", "Ana Costa");
    Bolsa b5 = criar_bolsa("BOLSA-005", "O-",  500.0, "2026-09-20", "2026-11-20", "Pedro Almeida");

    lista_inserir(l, b1);
    ASSERT_MSG(l->tamanho == 1, "Apos inserir 1 bolsa, tamanho = 1");

    lista_inserir(l, b2);
    lista_inserir(l, b3);
    lista_inserir(l, b4);
    lista_inserir(l, b5);
    ASSERT_MSG(l->tamanho == 5, "Apos inserir 5 bolsas, tamanho = 5");

    /* Ordem da cabeça para cauda: 005, 004, 003, 002, 001 (inserção no início) */
    ASSERT_MSG(strcmp(l->cabeca->bolsa.id, "BOLSA-005") == 0,
               "Cabeca eh BOLSA-005 (ultima inserida)");

    No *ultimo = l->cabeca;
    while (ultimo->proximo != NULL) ultimo = ultimo->proximo;
    ASSERT_MSG(strcmp(ultimo->bolsa.id, "BOLSA-001") == 0,
               "Cauda eh BOLSA-001 (primeira inserida)");

    lista_liberar(l);
}

void test_consulta(void) {
    printf("\n[Consulta]\n");
    Lista *l = lista_criar();

    lista_inserir(l, criar_bolsa("BOLSA-001", "O+",  450.0, "2026-09-01", "2026-11-01", "Joao Silva"));
    lista_inserir(l, criar_bolsa("BOLSA-002", "A-",  450.0, "2026-09-05", "2026-11-05", "Maria Santos"));
    lista_inserir(l, criar_bolsa("BOLSA-003", "B+",  400.0, "2026-09-10", "2026-11-10", "Carlos Oliveira"));
    lista_inserir(l, criar_bolsa("BOLSA-004", "AB+", 450.0, "2026-08-15", "2026-10-15", "Ana Costa"));
    lista_inserir(l, criar_bolsa("BOLSA-005", "O-",  500.0, "2026-09-20", "2026-11-20", "Pedro Almeida"));

    /* Consulta existente */
    Bolsa *encontrada = lista_consultar(l, "BOLSA-003");
    ASSERT_MSG(encontrada != NULL, "BOLSA-003 encontrada");
    ASSERT_MSG(strcmp(encontrada->tipo_sanguineo, "B+") == 0, "BOLSA-003 tem tipo B+");
    ASSERT_MSG(strcmp(encontrada->doador, "Carlos Oliveira") == 0, "BOLSA-003 doador = Carlos Oliveira");

    /* Consulta inexistente */
    ASSERT_MSG(lista_consultar(l, "BOLSA-999") == NULL, "BOLSA-999 nao encontrada (NULL)");

    lista_liberar(l);
}

void test_remocao(void) {
    printf("\n[Remocao]\n");
    Lista *l = lista_criar();

    lista_inserir(l, criar_bolsa("BOLSA-001", "O+",  450.0, "2026-09-01", "2026-11-01", "Joao Silva"));
    lista_inserir(l, criar_bolsa("BOLSA-002", "A-",  450.0, "2026-09-05", "2026-11-05", "Maria Santos"));
    lista_inserir(l, criar_bolsa("BOLSA-003", "B+",  400.0, "2026-09-10", "2026-11-10", "Carlos Oliveira"));
    lista_inserir(l, criar_bolsa("BOLSA-004", "AB+", 450.0, "2026-08-15", "2026-10-15", "Ana Costa"));
    lista_inserir(l, criar_bolsa("BOLSA-005", "O-",  500.0, "2026-09-20", "2026-11-20", "Pedro Almeida"));

    /* Remover cabeça (BOLSA-005) */
    ASSERT_MSG(lista_remover(l, "BOLSA-005") == 1, "Remocao da cabeca (BOLSA-005) retorna 1");
    ASSERT_MSG(l->tamanho == 4, "Tamanho apos remover cabeca = 4");
    ASSERT_MSG(lista_consultar(l, "BOLSA-005") == NULL, "BOLSA-005 nao encontrada apos remocao");

    /* Remover do meio (BOLSA-003) */
    ASSERT_MSG(lista_remover(l, "BOLSA-003") == 1, "Remocao do meio (BOLSA-003) retorna 1");
    ASSERT_MSG(l->tamanho == 3, "Tamanho apos remover meio = 3");

    /* Remover cauda (BOLSA-001) */
    ASSERT_MSG(lista_remover(l, "BOLSA-001") == 1, "Remocao da cauda (BOLSA-001) retorna 1");
    ASSERT_MSG(l->tamanho == 2, "Tamanho apos remover cauda = 2");

    /* Remover inexistente */
    ASSERT_MSG(lista_remover(l, "BOLSA-999") == 0, "Remocao inexistente (BOLSA-999) retorna 0");
    ASSERT_MSG(l->tamanho == 2, "Tamanho inalterado apos remocao inexistente = 2");

    /* Verificar que restam apenas BOLSA-004 e BOLSA-002 */
    ASSERT_MSG(lista_consultar(l, "BOLSA-004") != NULL, "BOLSA-004 ainda presente");
    ASSERT_MSG(lista_consultar(l, "BOLSA-002") != NULL, "BOLSA-002 ainda presente");

    lista_liberar(l);
}

int main(void) {
    printf("=== Testes da Lista Encadeada (AED U1 — C) ===\n");

    test_lista_vazia();
    test_insercao();
    test_consulta();
    test_remocao();

    printf("\n=== Resultado: %d/%d testes passaram ===\n", testes_passaram, testes_total);

    if (testes_passaram == testes_total) {
        printf("SUCESSO: Todos os testes passaram sem erros de memoria!\n");
        return EXIT_SUCCESS;
    } else {
        printf("FALHA: Alguns testes falharam.\n");
        return EXIT_FAILURE;
    }
}
