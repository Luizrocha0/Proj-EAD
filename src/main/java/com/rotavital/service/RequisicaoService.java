package com.rotavital.service;

import com.rotavital.aed.u1.FilaRequisicoes;
import com.rotavital.domain.Requisicao;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Serviço de aplicação para gestão da fila de requisições emergenciais.
 * Delega operações para a estrutura AED real ({@link FilaRequisicoes}).
 */
@Service
public class RequisicaoService {

    private final FilaRequisicoes fila = new FilaRequisicoes();

    /**
     * Enfileira uma nova requisição.
     */
    public void enfileirar(Requisicao requisicao) {
        fila.enfileirar(requisicao);
    }

    /**
     * Desenfileira a próxima requisição (FIFO).
     */
    public Optional<Requisicao> atenderProxima() {
        return fila.desenfileirar();
    }

    /**
     * Consulta a próxima requisição sem removê-la.
     */
    public Optional<Requisicao> espiarProxima() {
        return fila.espiar();
    }

    /**
     * Retorna a quantidade de requisições na fila.
     */
    public int tamanho() {
        return fila.getTamanho();
    }

    /**
     * Lista todas as requisições na fila.
     */
    public Requisicao[] listarTodas() {
        return fila.toArray();
    }
}
