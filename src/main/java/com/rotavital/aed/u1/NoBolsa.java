package com.rotavital.aed.u1;

import com.rotavital.domain.Bolsa;

/**
 * Nó da lista encadeada de bolsas de sangue.
 * Implementação própria — não usa coleções nativas do Java.
 *
 * Equivalente C: struct No { Bolsa bolsa; struct No *proximo; }
 */
class NoBolsa {

    Bolsa dado;
    NoBolsa proximo;

    NoBolsa(Bolsa dado) {
        this.dado = dado;
        this.proximo = null;
    }
}
