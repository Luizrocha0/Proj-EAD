package com.rotavital.aed.u2;

public final class Ordenacoes {
    private Ordenacoes() {
    }

    public static void bubbleSort(int[] valores) {
        for (int fim = valores.length - 1; fim > 0; fim--) {
            boolean trocou = false;
            for (int i = 0; i < fim; i++) {
                if (valores[i] > valores[i + 1]) {
                    trocar(valores, i, i + 1);
                    trocou = true;
                }
            }
            if (!trocou) {
                return;
            }
        }
    }

    public static void selectionSort(int[] valores) {
        for (int i = 0; i < valores.length - 1; i++) {
            int menor = i;
            for (int j = i + 1; j < valores.length; j++) {
                if (valores[j] < valores[menor]) {
                    menor = j;
                }
            }
            if (menor != i) {
                trocar(valores, i, menor);
            }
        }
    }

    public static void insertionSort(int[] valores) {
        for (int i = 1; i < valores.length; i++) {
            int chave = valores[i];
            int posicao = i;
            while (posicao > 0 && valores[posicao - 1] > chave) {
                valores[posicao] = valores[posicao - 1];
                posicao--;
            }
            valores[posicao] = chave;
        }
    }

    public static void mergeSort(int[] valores) {
        if (valores.length > 1) {
            mergeSort(valores, new int[valores.length], 0, valores.length);
        }
    }

    private static void mergeSort(int[] valores, int[] auxiliar, int inicio, int fim) {
        if (fim - inicio < 2) {
            return;
        }
        int meio = inicio + (fim - inicio) / 2;
        mergeSort(valores, auxiliar, inicio, meio);
        mergeSort(valores, auxiliar, meio, fim);

        int esquerda = inicio;
        int direita = meio;
        for (int i = inicio; i < fim; i++) {
            if (direita == fim || (esquerda < meio && valores[esquerda] <= valores[direita])) {
                auxiliar[i] = valores[esquerda++];
            } else {
                auxiliar[i] = valores[direita++];
            }
        }
        System.arraycopy(auxiliar, inicio, valores, inicio, fim - inicio);
    }

    public static void quickSort(int[] valores) {
        quickSort(valores, 0, valores.length);
    }

    private static void quickSort(int[] valores, int inicio, int fim) {
        while (fim - inicio > 1) {
            int pivo = valores[inicio + (fim - inicio) / 2];
            int menores = inicio;
            int maiores = fim;
            int atual = inicio;
            while (atual < maiores) {
                if (valores[atual] < pivo) {
                    trocar(valores, menores++, atual++);
                } else if (valores[atual] > pivo) {
                    trocar(valores, atual, --maiores);
                } else {
                    atual++;
                }
            }

            // A chamada recursiva recebe a partição menor. A maior reaproveita o quadro atual.
            if (menores - inicio < fim - maiores) {
                quickSort(valores, inicio, menores);
                inicio = maiores;
            } else {
                quickSort(valores, maiores, fim);
                fim = menores;
            }
        }
    }

    public static void heapSort(int[] valores) {
        for (int raiz = valores.length / 2 - 1; raiz >= 0; raiz--) {
            descerNoHeap(valores, raiz, valores.length);
        }
        for (int fim = valores.length - 1; fim > 0; fim--) {
            trocar(valores, 0, fim);
            descerNoHeap(valores, 0, fim);
        }
    }

    private static void descerNoHeap(int[] valores, int raiz, int tamanho) {
        while (raiz < tamanho / 2) {
            int filho = raiz * 2 + 1;
            int direito = filho + 1;
            if (direito < tamanho && valores[direito] > valores[filho]) {
                filho = direito;
            }
            if (valores[raiz] >= valores[filho]) {
                return;
            }
            trocar(valores, raiz, filho);
            raiz = filho;
        }
    }

    private static void trocar(int[] valores, int primeira, int segunda) {
        int temporario = valores[primeira];
        valores[primeira] = valores[segunda];
        valores[segunda] = temporario;
    }
}
