package com.rotavital.storage;

import com.rotavital.aed.u1.ListaEstoque;
import com.rotavital.domain.Bolsa;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Component
@Profile({"u1", "default"})
public class ListaEstoqueStorage implements EstoqueStorage {

    private final ListaEstoque listaEstoque = new ListaEstoque();

    @Override
    public void salvar(Bolsa bolsa) {
        java.util.Objects.requireNonNull(bolsa, "bolsa não pode ser nula");
        listaEstoque.inserir(bolsa);
    }

    @Override
    public Optional<Bolsa> buscarPorId(String id) {
        if (id == null || id.isBlank() || id.trim().length() > 50) {
            return Optional.empty();
        }
        return listaEstoque.consultar(id.trim());
    }

    @Override
    public boolean removerPorId(String id) {
        if (id == null || id.isBlank() || id.trim().length() > 50) {
            return false;
        }
        return listaEstoque.removerPorId(id.trim());
    }

    @Override
    public List<Bolsa> listar() {
        return new java.util.ArrayList<>(Arrays.asList(listaEstoque.toArray()));
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
