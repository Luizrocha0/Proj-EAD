/**
 * test_fila.c — Testes da fila encadeada FIFO de requisições.
 *
 * Usa as mesmas fixtures de fixtures/requisicoes_u1.json e os mesmos
 * resultados esperados de fixtures/resultados_esperados_u1.json.
 *
 * Cenários idênticos ao FilaRequisicaoTest.java:
 * - Fila vazia
 * - Enfileiramento de 5 requisições (ordem FIFO preservada)
 * - Desenfileiramento preserva ordem
 * - Fila que esvazia e volta a ser preenchida
 *
 * Compilar com: gcc -Wall -Wextra -std=c11 -fsanitize=address,undefined
 *               -Iinclude src/u1/fila.c tests/test_fila.c -o tests/test_fila
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "../include/fila.h"

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

/* ── Fixtures (mesmos dados de fixtures/requisicoes_u1.json) ── */
static Requisicao criar_requisicao(const char *id, const char *hospital,
                                    const char *tipo, int qtd, Prioridade prio) {
    Requisicao r;
    strncpy(r.id, id, sizeof(r.id) - 1);
    r.id[sizeof(r.id) - 1] = '\0';
    strncpy(r.hospital_id, hospital, sizeof(r.hospital_id) - 1);
    r.hospital_id[sizeof(r.hospital_id) - 1] = '\0';
    strncpy(r.tipo_sanguineo, tipo, sizeof(r.tipo_sanguineo) - 1);
    r.tipo_sanguineo[sizeof(r.tipo_sanguineo) - 1] = '\0';
    r.quantidade_bolsas = qtd;
    r.prioridade = prio;
    return r;
}

/* ── Testes ── */

void test_fila_vazia(void) {
    printf("\n[Fila vazia]\n");
    Fila *f = fila_criar();
    Requisicao out;

    ASSERT_MSG(f->tamanho == 0, "Fila recem-criada tem tamanho 0");
    ASSERT_MSG(f->cabeca == NULL, "Cabeca eh NULL em fila vazia");
    ASSERT_MSG(f->cauda == NULL, "Cauda eh NULL em fila vazia");
    ASSERT_MSG(fila_desenfileirar(f, &out) == 0, "Desenfileirar fila vazia retorna 0");

    fila_liberar(f);
}

void test_enfileiramento(void) {
    printf("\n[Enfileiramento]\n");
    Fila *f = fila_criar();

    fila_enfileirar(f, criar_requisicao("REQ-001", "HOSP-CENTRAL", "O+",  2, EMERGENCIA));
    ASSERT_MSG(f->tamanho == 1, "Apos enfileirar 1, tamanho = 1");

    fila_enfileirar(f, criar_requisicao("REQ-002", "HOSP-NORTE",   "A-",  1, URGENTE));
    fila_enfileirar(f, criar_requisicao("REQ-003", "HOSP-SUL",     "B+",  3, NORMAL));
    fila_enfileirar(f, criar_requisicao("REQ-004", "HOSP-LESTE",   "AB+", 1, URGENTE));
    fila_enfileirar(f, criar_requisicao("REQ-005", "HOSP-OESTE",   "O-",  4, EMERGENCIA));

    ASSERT_MSG(f->tamanho == 5, "Apos enfileirar 5, tamanho = 5");

    /* FIFO: cabeca deve ser REQ-001 (primeiro enfileirado) */
    ASSERT_MSG(strcmp(f->cabeca->requisicao.id, "REQ-001") == 0,
               "Cabeca eh REQ-001 (primeiro enfileirado)");

    /* Cauda deve ser REQ-005 (ultimo enfileirado) */
    ASSERT_MSG(strcmp(f->cauda->requisicao.id, "REQ-005") == 0,
               "Cauda eh REQ-005 (ultimo enfileirado)");

    fila_liberar(f);
}

void test_desenfileiramento(void) {
    printf("\n[Desenfileiramento]\n");
    Fila *f = fila_criar();
    Requisicao out;

    fila_enfileirar(f, criar_requisicao("REQ-001", "HOSP-CENTRAL", "O+",  2, EMERGENCIA));
    fila_enfileirar(f, criar_requisicao("REQ-002", "HOSP-NORTE",   "A-",  1, URGENTE));
    fila_enfileirar(f, criar_requisicao("REQ-003", "HOSP-SUL",     "B+",  3, NORMAL));
    fila_enfileirar(f, criar_requisicao("REQ-004", "HOSP-LESTE",   "AB+", 1, URGENTE));
    fila_enfileirar(f, criar_requisicao("REQ-005", "HOSP-OESTE",   "O-",  4, EMERGENCIA));

    /* Primeiro desenfileiramento: REQ-001 */
    ASSERT_MSG(fila_desenfileirar(f, &out) == 1, "Primeiro desenfileiramento retorna 1");
    ASSERT_MSG(strcmp(out.id, "REQ-001") == 0, "Primeiro desenfileirado = REQ-001");
    ASSERT_MSG(strcmp(out.hospital_id, "HOSP-CENTRAL") == 0, "Hospital = HOSP-CENTRAL");
    ASSERT_MSG(f->tamanho == 4, "Tamanho apos primeiro desenfileiramento = 4");

    /* Segundo desenfileiramento: REQ-002 */
    ASSERT_MSG(fila_desenfileirar(f, &out) == 1, "Segundo desenfileiramento retorna 1");
    ASSERT_MSG(strcmp(out.id, "REQ-002") == 0, "Segundo desenfileirado = REQ-002");
    ASSERT_MSG(strcmp(out.hospital_id, "HOSP-NORTE") == 0, "Hospital = HOSP-NORTE");
    ASSERT_MSG(f->tamanho == 3, "Tamanho apos segundo desenfileiramento = 3");

    /* Desenfileirar todos os restantes */
    ASSERT_MSG(fila_desenfileirar(f, &out) == 1 && strcmp(out.id, "REQ-003") == 0,
               "Terceiro = REQ-003");
    ASSERT_MSG(fila_desenfileirar(f, &out) == 1 && strcmp(out.id, "REQ-004") == 0,
               "Quarto = REQ-004");
    ASSERT_MSG(fila_desenfileirar(f, &out) == 1 && strcmp(out.id, "REQ-005") == 0,
               "Quinto = REQ-005");

    ASSERT_MSG(f->tamanho == 0, "Fila vazia apos desenfileirar todos");
    ASSERT_MSG(f->cabeca == NULL, "Cabeca NULL apos esvaziar");
    ASSERT_MSG(f->cauda == NULL, "Cauda NULL apos esvaziar");

    /* Tentar desenfileirar fila já vazia */
    ASSERT_MSG(fila_desenfileirar(f, &out) == 0, "Desenfileirar fila ja vazia retorna 0");

    fila_liberar(f);
}

void test_reuso(void) {
    printf("\n[Reuso da fila]\n");
    Fila *f = fila_criar();
    Requisicao out;

    fila_enfileirar(f, criar_requisicao("REQ-001", "HOSP-CENTRAL", "O+", 2, EMERGENCIA));
    fila_desenfileirar(f, &out);
    ASSERT_MSG(f->tamanho == 0, "Fila vazia apos usar e esvaziar");

    /* Preencher novamente */
    fila_enfileirar(f, criar_requisicao("REQ-005", "HOSP-OESTE", "O-", 4, EMERGENCIA));
    fila_enfileirar(f, criar_requisicao("REQ-003", "HOSP-SUL",   "B+", 3, NORMAL));
    ASSERT_MSG(f->tamanho == 2, "Tamanho = 2 apos reuso");

    fila_desenfileirar(f, &out);
    ASSERT_MSG(strcmp(out.id, "REQ-005") == 0, "Primeiro no reuso = REQ-005");

    fila_liberar(f);
}

void test_espiar(void) {
    printf("\n[Espiar fila]\n");
    Fila *f = fila_criar();
    Requisicao out;

    /* Teste com fila vazia */
    ASSERT_MSG(fila_espiar(f, &out) == 0, "Espiar fila vazia retorna 0");

    /* Teste consultando o primeiro elemento */
    fila_enfileirar(f, criar_requisicao("REQ-001", "HOSP-CENTRAL", "O+", 2, EMERGENCIA));
    fila_enfileirar(f, criar_requisicao("REQ-002", "HOSP-NORTE",   "A-", 1, URGENTE));
    
    ASSERT_MSG(fila_espiar(f, &out) == 1, "Espiar com elementos retorna 1");
    ASSERT_MSG(strcmp(out.id, "REQ-001") == 0, "Elemento espiado eh o primeiro enfileirado (REQ-001)");
    
    /* Confirmacao de que consultar nao altera o tamanho */
    ASSERT_MSG(f->tamanho == 2, "Tamanho apos espiar continua sendo 2 (nao removeu)");
    
    /* Confirmar se a fila continua igual */
    ASSERT_MSG(strcmp(f->cabeca->requisicao.id, "REQ-001") == 0, "Cabeca continua sendo REQ-001");

    fila_liberar(f);
}

int main(void) {
    printf("=== Testes da Fila FIFO (AED U1 — C) ===\n");

    test_fila_vazia();
    test_enfileiramento();
    test_desenfileiramento();
    test_reuso();
    test_espiar();

    printf("\n=== Resultado: %d/%d testes passaram ===\n", testes_passaram, testes_total);

    if (testes_passaram == testes_total) {
        printf("SUCESSO: Todos os testes passaram sem erros de memoria!\n");
        return EXIT_SUCCESS;
    } else {
        printf("FALHA: Alguns testes falharam.\n");
        return EXIT_FAILURE;
    }
}
