package com.rotavital.requisicoes.negocio.aplicacao;

import com.rotavital.requisicoes.negocio.dominio.Requisicao;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class RequisicaoService {
    private final FilaDeRequisicoes fila;

    public RequisicaoService(FilaDeRequisicoes fila) {
        this.fila = fila;
    }

    public int enfileirar(Requisicao requisicao) {
        return fila.enfileirar(requisicao);
    }

    public <T> Optional<FilaDeRequisicoes.Consumo<T>> atenderProxima(Function<Requisicao, T> atendimento) {
        return fila.consumirProxima(atendimento);
    }

    public Optional<Requisicao> espiarProxima() {
        return fila.proxima();
    }

    public List<Requisicao> listarTodas() {
        return fila.listar();
    }
}
