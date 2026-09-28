#ifndef REQUISICAO_H
#define REQUISICAO_H

/**
 * Prioridade da requisição emergencial.
 * Equivale ao enum Requisicao.Prioridade em Java.
 */
typedef enum {
    NORMAL = 0,
    URGENTE = 1,
    EMERGENCIA = 2
} Prioridade;

/**
 * Estrutura que representa uma requisição emergencial de sangue.
 * Mesmos campos da classe Requisicao.java no domínio Java.
 */
typedef struct {
    char id[20];              /* ex: "REQ-001" */
    char hospital_id[20];     /* ex: "HOSP-CENTRAL" */
    char tipo_sanguineo[5];   /* ex: "O+", "AB-" */
    int quantidade_bolsas;    /* ex: 2 */
    Prioridade prioridade;    /* NORMAL, URGENTE, EMERGENCIA */
} Requisicao;

#endif /* REQUISICAO_H */
