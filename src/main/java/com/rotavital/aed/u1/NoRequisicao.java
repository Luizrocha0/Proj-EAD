package com.rotavital.aed.u1;

import com.rotavital.domain.Requisicao;

/**
 * Nó da fila encadeada de requisições emergenciais.
 * Implementação própria — não usa coleções nativas do Java.
 *
 * Equivalente C: reutiliza struct No com campo Requisicao
 */
class NoRequisicao {

    Requisicao dado;
    NoRequisicao proximo;

    NoRequisicao(Requisicao dado) {
        this.dado = dado;
        this.proximo = null;
    }
}
