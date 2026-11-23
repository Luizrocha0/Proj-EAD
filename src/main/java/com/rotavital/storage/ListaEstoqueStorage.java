package com.rotavital.storage;

import com.rotavital.aed.u1.ListaEstoque;
import com.rotavital.aed.u2.AlocacaoFefo;
import com.rotavital.domain.Bolsa;
import com.rotavital.domain.TipoSanguineo;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Component
@Profile({"u1", "default"})
public class ListaEstoqueStorage implements EstoqueStorage {

    private final ListaEstoque listaEstoque = new ListaEstoque();

    @Override
    public synchronized void salvar(Bolsa bolsa) {
        java.util.Objects.requireNonNull(bolsa, "bolsa não pode ser nula");
        listaEstoque.inserir(bolsa);
    }

    @Override
    public synchronized Optional<Bolsa> buscarPorId(String id) {
        if (id == null || id.isBlank() || id.trim().length() > 50) {
            return Optional.empty();
        }
        return listaEstoque.consultar(id.trim());
    }

    @Override
    public synchronized boolean removerPorId(String id) {
        if (id == null || id.isBlank() || id.trim().length() > 50) {
            return false;
        }
        return listaEstoque.removerPorId(id.trim());
    }

    @Override
    public synchronized List<Bolsa> alocar(TipoSanguineo receptor, int quantidade, LocalDate referencia) {
        Bolsa[] estoque = listaEstoque.toArray();
        List<Integer> selecionadas = AlocacaoFefo.selecionar(estoque, receptor, quantidade, referencia);
        if (selecionadas.isEmpty()) {
            return List.of();
        }

        boolean[] retirar = new boolean[estoque.length];
        var alocadas = new ArrayList<Bolsa>(selecionadas.size());
        for (int posicao : selecionadas) {
            retirar[posicao] = true;
            alocadas.add(estoque[posicao]);
        }
        listaEstoque.removerPosicoes(retirar);
        return List.copyOf(alocadas);
    }

    @Override
    public synchronized List<Bolsa> listar() {
        return new ArrayList<>(Arrays.asList(listaEstoque.toArray()));
    }

    @Override
    public synchronized Bolsa[] listarTodas() {
        return listaEstoque.toArray();
    }

    @Override
    public synchronized int tamanho() {
        return listaEstoque.getTamanho();
    }
}
