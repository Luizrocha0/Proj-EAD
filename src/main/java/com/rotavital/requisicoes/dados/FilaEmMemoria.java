package com.rotavital.requisicoes.dados;

import com.rotavital.aed.u1.FilaRequisicoes;
import com.rotavital.comum.dominio.ConflitoAtendimentoException;
import com.rotavital.requisicoes.negocio.aplicacao.FilaDeRequisicoes;
import com.rotavital.requisicoes.negocio.dominio.Requisicao;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

@Component
@Profile({"u1", "default"})
public class FilaEmMemoria implements FilaDeRequisicoes {
    private final FilaRequisicoes fila = new FilaRequisicoes();
    private final Set<String> pendentes = new HashSet<>();

    @Override
    public synchronized int enfileirar(Requisicao requisicao) {
        if (!pendentes.add(requisicao.getId())) {
            throw new ConflitoAtendimentoException("Já existe uma requisição pendente com esse ID");
        }
        fila.enfileirar(requisicao);
        return fila.getTamanho();
    }

    @Override
    public synchronized Optional<Requisicao> proxima() {
        return fila.espiar();
    }

    @Override
    public synchronized List<Requisicao> listar() {
        return List.of(fila.toArray());
    }

    @Override
    public synchronized <T> Optional<Consumo<T>> consumirProxima(Function<Requisicao, T> atendimento) {
        return fila.espiar().map(requisicao -> {
            T resultado = Objects.requireNonNull(atendimento.apply(requisicao));
            fila.desenfileirar();
            pendentes.remove(requisicao.getId());
            return new Consumo<>(resultado, fila.getTamanho());
        });
    }
}
