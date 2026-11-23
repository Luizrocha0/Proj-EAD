package com.rotavital.aed.u2;

import java.util.function.IntPredicate;

public final class Buscas {
    private Buscas() {
    }

    public static int sequencial(int[] valores, int alvo) {
        for (int i = 0; i < valores.length; i++) {
            if (valores[i] == alvo) {
                return i;
            }
        }
        return -1;
    }

    public static int binaria(int[] valoresOrdenados, int alvo) {
        int posicao = limiteInferior(valoresOrdenados.length, i -> valoresOrdenados[i] < alvo);
        return posicao < valoresOrdenados.length && valoresOrdenados[posicao] == alvo ? posicao : -1;
    }

    static int limiteInferior(int tamanho, IntPredicate precedeAlvo) {
        // Intervalo semiaberto. A primeira posição que não precede o alvo serve também à inserção FEFO.
        int inicio = 0;
        int fim = tamanho;
        while (inicio < fim) {
            int meio = inicio + (fim - inicio) / 2;
            if (precedeAlvo.test(meio)) {
                inicio = meio + 1;
            } else {
                fim = meio;
            }
        }
        return inicio;
    }
}
