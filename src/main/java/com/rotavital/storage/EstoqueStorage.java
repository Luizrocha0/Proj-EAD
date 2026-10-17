package com.rotavital.storage;

import com.rotavital.domain.Bolsa;
import java.util.List;
import java.util.Optional;

public interface EstoqueStorage {
    void salvar(Bolsa bolsa);
    Optional<Bolsa> buscarPorId(String id);
    boolean removerPorId(String id);
    default List<Bolsa> listar() {
        throw new UnsupportedOperationException("Stub para ciclo TDD Red T2.1");
    }
    Bolsa[] listarTodas();
    int tamanho();
}
