package com.rotavital.requisicoes.negocio.aplicacao;

import com.rotavital.requisicoes.negocio.dominio.Requisicao;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public interface FilaDeRequisicoes {
    int enfileirar(Requisicao requisicao);
    Optional<Requisicao> proxima();
    List<Requisicao> listar();
    <T> Optional<Consumo<T>> consumirProxima(Function<Requisicao, T> atendimento);

    record Consumo<T>(T resultado, int restantes) {
    }
}
