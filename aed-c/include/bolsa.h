#ifndef BOLSA_H
#define BOLSA_H

/**
 * Estrutura que representa uma bolsa de sangue.
 * Mesmos campos da classe Bolsa.java no domínio Java.
 * Dados fixos (strings) para simplificar malloc/free nesta U1.
 */
typedef struct {
    char id[20];              /* ex: "BOLSA-001" */
    char tipo_sanguineo[5];   /* ex: "O+", "AB-" */
    double volume_ml;         /* ex: 450.0 */
    char data_coleta[11];     /* "AAAA-MM-DD" */
    char validade[11];        /* "AAAA-MM-DD" */
    char doador[50];          /* ex: "João Silva" */
} Bolsa;

#endif /* BOLSA_H */
