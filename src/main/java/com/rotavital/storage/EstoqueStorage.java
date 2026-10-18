package com.rotavital.storage;

import com.rotavital.domain.Bolsa;
import java.util.List;
import java.util.Optional;

public interface EstoqueStorage {
    void salvar(Bolsa bolsa);
    Optional<Bolsa> buscarPorId(String id);
    boolean removerPorId(String id);
    List<Bolsa> listar();
    Bolsa[] listarTodas();
    int tamanho();
}
