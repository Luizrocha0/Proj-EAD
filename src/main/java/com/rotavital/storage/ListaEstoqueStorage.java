package com.rotavital.storage;

import com.rotavital.domain.Bolsa;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Profile({"u1", "default"})
public class ListaEstoqueStorage implements EstoqueStorage {

    @Override
    public void salvar(Bolsa bolsa) {
        throw new UnsupportedOperationException("Stub para ciclo TDD Red");
    }

    @Override
    public Optional<Bolsa> buscarPorId(String id) {
        throw new UnsupportedOperationException("Stub para ciclo TDD Red");
    }

    @Override
    public boolean removerPorId(String id) {
        throw new UnsupportedOperationException("Stub para ciclo TDD Red");
    }

    @Override
    public Bolsa[] listarTodas() {
        throw new UnsupportedOperationException("Stub para ciclo TDD Red");
    }

    @Override
    public int tamanho() {
        throw new UnsupportedOperationException("Stub para ciclo TDD Red");
    }
}
