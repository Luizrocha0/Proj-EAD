package com.rotavital.aed.u1;

import com.rotavital.domain.Requisicao;

import java.util.Optional;

/**
 * Fila simplesmente encadeada de requisições emergenciais (FIFO).
 *
 * Estrutura pedagógica com nós próprios ({@link NoRequisicao}).
 * Não utiliza Queue, LinkedList ou qualquer coleção nativa do Java.
 *
 * <h3>Equivalência C (aed-c/include/fila.h)</h3>
 * <pre>
 * fila_criar()          → new FilaRequisicoes()
 * fila_enfileirar()     → enfileirar(Requisicao)
 * fila_desenfileirar()  → desenfileirar()
 * fila_liberar()        → (garbage collector)
 * </pre>
 *
 * A fila mantém dois ponteiros: {@code cabeca} (onde desenfileira) e
 * {@code cauda} (onde enfileira). Isso garante O(1) para ambas as operações.
 *
 * Em C, cada nó é alocado com {@code malloc} e liberado com {@code free}
 * no desenfileirar e no liberar.
 */
public class FilaRequisicoes {

    /** Ponteiro para o primeiro nó — onde se desenfileira (equivalente a Fila->cabeca em C) */
    private NoRequisicao cabeca;

    /** Ponteiro para o último nó — onde se enfileira (equivalente a Fila->cauda em C) */
    private NoRequisicao cauda;

    /** Quantidade de elementos na fila */
    private int tamanho;

    public FilaRequisicoes() {
        this.cabeca = null;
        this.cauda = null;
        this.tamanho = 0;
    }

    /**
     * Enfileira uma requisição no final da fila (FIFO).
     *
     * Equivalente C: fila_enfileirar(Fila *f, Requisicao r)
     * - Aloca novo nó: No *novo = malloc(sizeof(No));
     * - Se fila vazia: f->cabeca = f->cauda = novo
     * - Senão: f->cauda->proximo = novo; f->cauda = novo
     *
     * Complexidade: O(1)
     */
    public void enfileirar(Requisicao r) {
        NoRequisicao novo = new NoRequisicao(r);  // Em C: No *novo = malloc(sizeof(No));

        if (cauda == null) {
            // Fila vazia: cabeca e cauda apontam para o mesmo nó
            // Em C: f->cabeca = f->cauda = novo;
            cabeca = novo;
            cauda = novo;
        } else {
            // Insere no final: cauda->proximo aponta para o novo nó
            // Em C: f->cauda->proximo = novo; f->cauda = novo;
            cauda.proximo = novo;
            cauda = novo;
        }
        tamanho++;
    }

    /**
     * Desenfileira a requisição do início da fila (FIFO).
     *
     * Equivalente C: int fila_desenfileirar(Fila *f, Requisicao *out)
     * - Se fila vazia: return 0 (falha)
     * - *out = f->cabeca->requisicao
     * - No *temp = f->cabeca; f->cabeca = f->cabeca->proximo; free(temp)
     * - Se f->cabeca == NULL: f->cauda = NULL (fila ficou vazia)
     *
     * Complexidade: O(1)
     *
     * @return Optional com a requisição removida, ou empty se fila vazia
     */
    public Optional<Requisicao> desenfileirar() {
        if (cabeca == null) {
            return Optional.empty();     // Em C: return 0; (fila vazia)
        }

        Requisicao dado = cabeca.dado;   // Em C: *out = f->cabeca->requisicao;
        // Em C: No *temp = f->cabeca; f->cabeca = f->cabeca->proximo; free(temp);
        cabeca = cabeca.proximo;

        if (cabeca == null) {
            // Fila ficou vazia: atualizar cauda também
            // Em C: if (f->cabeca == NULL) f->cauda = NULL;
            cauda = null;
        }
        tamanho--;
        return Optional.of(dado);
    }

    /**
     * Consulta a requisição no início da fila sem remover (peek).
     *
     * Complexidade: O(1)
     */
    public Optional<Requisicao> espiar() {
        if (cabeca == null) {
            return Optional.empty();
        }
        return Optional.of(cabeca.dado);
    }

    /**
     * Retorna o tamanho atual da fila.
     * Equivalente C: f->tamanho
     */
    public int getTamanho() {
        return tamanho;
    }

    /**
     * Verifica se a fila está vazia.
     * Equivalente C: f->cabeca == NULL
     */
    public boolean isVazia() {
        return cabeca == null;
    }

    /**
     * Retorna todas as requisições como array (para serialização/endpoints).
     * Complexidade: O(n)
     */
    public Requisicao[] toArray() {
        Requisicao[] resultado = new Requisicao[tamanho];
        NoRequisicao atual = cabeca;
        int i = 0;
        while (atual != null) {
            resultado[i++] = atual.dado;
            atual = atual.proximo;
        }
        return resultado;
    }
}
