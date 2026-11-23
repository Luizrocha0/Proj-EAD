package com.rotavital.aed.u2;

import com.rotavital.domain.TipoSanguineo;

import java.util.ArrayList;
import java.util.List;

final class IndiceEstoque {
    private final Entrada[] baldes = new Entrada[5];

    void inserir(TipoSanguineo tipo, int posicao) {
        int balde = balde(tipo);
        Entrada entrada = encontrar(tipo);
        if (entrada == null) {
            entrada = new Entrada(tipo, baldes[balde]);
            baldes[balde] = entrada;
        }
        entrada.posicoes.add(posicao);
    }

    List<Integer> buscar(TipoSanguineo tipo) {
        Entrada entrada = encontrar(tipo);
        return entrada == null ? List.of() : entrada.posicoes;
    }

    private Entrada encontrar(TipoSanguineo tipo) {
        // A colisão liga chaves distintas. Posições repetidas por tipo ficam na mesma entrada.
        for (Entrada atual = baldes[balde(tipo)]; atual != null; atual = atual.proxima) {
            if (atual.tipo == tipo) {
                return atual;
            }
        }
        return null;
    }

    private int balde(TipoSanguineo tipo) {
        return tipo.ordinal() % baldes.length;
    }

    private static final class Entrada {
        private final TipoSanguineo tipo;
        private final List<Integer> posicoes = new ArrayList<>();
        private final Entrada proxima;

        private Entrada(TipoSanguineo tipo, Entrada proxima) {
            this.tipo = tipo;
            this.proxima = proxima;
        }
    }
}
