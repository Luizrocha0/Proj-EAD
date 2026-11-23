package com.rotavital.storage;

import com.rotavital.domain.Bolsa;
import com.rotavital.domain.TipoSanguineo;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EstoqueStorage {
    void salvar(Bolsa bolsa);
    Optional<Bolsa> buscarPorId(String id);
    boolean removerPorId(String id);
    List<Bolsa> alocar(TipoSanguineo receptor, int quantidade, LocalDate referencia);
    List<Bolsa> listar();
    Bolsa[] listarTodas();
    int tamanho();
}
