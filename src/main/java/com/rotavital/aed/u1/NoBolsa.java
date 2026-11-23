package com.rotavital.aed.u1;

import com.rotavital.estoque.negocio.dominio.Bolsa;

/**
 * Nó da lista encadeada de bolsas de sangue.
 * Implementação própria — não usa coleções nativas do Java.
 *
 * Equivalente C: struct No { Bolsa bolsa; struct No *proximo; }
 */
class NoBolsa {

    final long chave;
    final Bolsa dado;
    NoBolsa proximo;

    NoBolsa(long chave, Bolsa dado) {
        this.chave = chave;
        this.dado = dado;
        this.proximo = null;
    }
}
