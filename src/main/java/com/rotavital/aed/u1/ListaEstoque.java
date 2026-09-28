package com.rotavital.aed.u1;

import com.rotavital.domain.Bolsa;

import java.util.Optional;

/**
 * Lista simplesmente encadeada de bolsas de sangue (estoque).
 *
 * Estrutura pedagógica com nós próprios ({@link NoBolsa}).
 * Não utiliza ArrayList, LinkedList ou qualquer coleção nativa do Java.
 *
 * <h3>Equivalência C (aed-c/include/lista.h)</h3>
 * <pre>
 * lista_criar()     → new ListaEstoque()
 * lista_inserir()   → inserir(Bolsa)
 * lista_consultar() → consultar(String id)
 * lista_remover()   → remover(String id)
 * lista_liberar()   → (garbage collector — sem equivalente direto)
 * </pre>
 *
 * Em C, cada {@code No} é alocado com {@code malloc} e liberado com {@code free}.
 * Em Java, o garbage collector cuida da desalocação, mas a lógica de
 * encadeamento (cabeça → próximo → próximo → null) é idêntica.
 */
public class ListaEstoque {

    /** Ponteiro para o primeiro nó da lista (equivalente a Lista->cabeca em C) */
    private NoBolsa cabeca;

    /** Quantidade de elementos na lista */
    private int tamanho;

    public ListaEstoque() {
        this.cabeca = null;
        this.tamanho = 0;
    }

    /**
     * Insere uma bolsa no início da lista.
     *
     * Equivalente C: lista_inserir(Lista *l, Bolsa b)
     * - Aloca novo nó com malloc
     * - novo->proximo = l->cabeca
     * - l->cabeca = novo
     * - l->tamanho++
     *
     * Complexidade: O(1)
     */
    public void inserir(Bolsa b) {
        NoBolsa novo = new NoBolsa(b);  // Em C: No *novo = malloc(sizeof(No));
        novo.proximo = cabeca;           // Em C: novo->proximo = l->cabeca;
        cabeca = novo;                   // Em C: l->cabeca = novo;
        tamanho++;                       // Em C: l->tamanho++;
    }

    /**
     * Consulta uma bolsa pelo ID, percorrendo a lista.
     *
     * Equivalente C: Bolsa* lista_consultar(Lista *l, const char *id)
     * - Percorre a lista: atual = l->cabeca; while (atual != NULL)
     * - Compara com strcmp(atual->bolsa.id, id) == 0
     * - Retorna ponteiro para a bolsa ou NULL
     *
     * Complexidade: O(n)
     */
    public Optional<Bolsa> consultar(String id) {
        NoBolsa atual = cabeca;          // Em C: No *atual = l->cabeca;
        while (atual != null) {          // Em C: while (atual != NULL)
            if (atual.dado.getId().equals(id)) {  // Em C: strcmp(atual->bolsa.id, id) == 0
                return Optional.of(atual.dado);
            }
            atual = atual.proximo;       // Em C: atual = atual->proximo;
        }
        return Optional.empty();         // Em C: return NULL;
    }

    /**
     * Remove uma bolsa pelo ID.
     *
     * Equivalente C: int lista_remover(Lista *l, const char *id)
     * - Caso especial: remoção da cabeça
     * - Caso geral: encontra o nó anterior e religa ponteiros
     * - Em C: free(removido) — libera a memória do nó
     *
     * Complexidade: O(n)
     *
     * @return true se a bolsa foi encontrada e removida, false caso contrário
     */
    public boolean remover(String id) {
        if (cabeca == null) {
            return false;
        }

        // Caso especial: remoção do primeiro nó (cabeça)
        // Em C: if (strcmp(l->cabeca->bolsa.id, id) == 0) { No *temp = l->cabeca; l->cabeca = l->cabeca->proximo; free(temp); }
        if (cabeca.dado.getId().equals(id)) {
            cabeca = cabeca.proximo;     // Em C: l->cabeca = l->cabeca->proximo; free(temp);
            tamanho--;
            return true;
        }

        // Caso geral: percorre até encontrar o nó anterior ao que será removido
        // Em C: No *anterior = l->cabeca; while (anterior->proximo != NULL)
        NoBolsa anterior = cabeca;
        while (anterior.proximo != null) {
            if (anterior.proximo.dado.getId().equals(id)) {
                // Em C: No *removido = anterior->proximo; anterior->proximo = removido->proximo; free(removido);
                anterior.proximo = anterior.proximo.proximo;
                tamanho--;
                return true;
            }
            anterior = anterior.proximo;
        }

        return false;
    }

    /**
     * Retorna o tamanho atual da lista.
     * Equivalente C: l->tamanho
     */
    public int getTamanho() {
        return tamanho;
    }

    /**
     * Verifica se a lista está vazia.
     * Equivalente C: l->cabeca == NULL
     */
    public boolean isVazia() {
        return cabeca == null;
    }

    /**
     * Retorna todas as bolsas como array (para serialização/endpoints).
     * Complexidade: O(n)
     */
    public Bolsa[] toArray() {
        Bolsa[] resultado = new Bolsa[tamanho];
        NoBolsa atual = cabeca;
        int i = 0;
        while (atual != null) {
            resultado[i++] = atual.dado;
            atual = atual.proximo;
        }
        return resultado;
    }
}
