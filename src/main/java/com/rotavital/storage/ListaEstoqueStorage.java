package com.rotavital.storage;

import com.rotavital.aed.u1.ListaEstoque;
import com.rotavital.domain.Bolsa;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Profile({"u1", "default"})
public class ListaEstoqueStorage implements EstoqueStorage {

    private final ListaEstoque listaEstoque = new ListaEstoque();

    @Override
    public void salvar(Bolsa bolsa) {
        listaEstoque.inserir(bolsa);
    }

    @Override
    public Optional<Bolsa> buscarPorId(String id) {
        return listaEstoque.consultar(id);
    }

    @Override
    public boolean removerPorId(String id) {
        return listaEstoque.remover(id);
    }

    @Override
    public Bolsa[] listarTodas() {
        return listaEstoque.toArray();
    }

    @Override
    public int tamanho() {
        return listaEstoque.getTamanho();
    }
}
