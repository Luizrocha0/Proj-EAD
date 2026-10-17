package com.rotavital.service;

import com.rotavital.domain.Bolsa;
import com.rotavital.storage.EstoqueStorage;
import com.rotavital.storage.ListaEstoqueStorage;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;

/**
 * Serviço de aplicação para gestão do estoque de bolsas de sangue.
 * Desacoplado via {@link EstoqueStorage}.
 *
 * Controllers apenas chamam este serviço — toda lógica de orquestração reside aqui.
 */
@Service
public class EstoqueService {

    private final EstoqueStorage storage;

    public EstoqueService() {
        this(new ListaEstoqueStorage());
    }

    public EstoqueService(EstoqueStorage storage) {
        this.storage = Objects.requireNonNull(storage, "storage não pode ser nulo");
    }

    /**
     * Adiciona uma bolsa ao estoque delegando para o storage.
     */
    public void adicionar(Bolsa bolsa) {
        Objects.requireNonNull(bolsa, "bolsa não pode ser nula");
        storage.salvar(bolsa);
    }

    /**
     * Consulta uma bolsa pelo ID delegando para o storage.
     */
    public Optional<Bolsa> consultar(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        return storage.buscarPorId(id.trim());
    }

    /**
     * Remove uma bolsa do estoque pelo ID delegando para o storage.
     *
     * @return true se encontrada e removida
     */
    public boolean remover(String id) {
        if (id == null || id.isBlank()) {
            return false;
        }
        return storage.removerPorId(id.trim());
    }

    /**
     * Retorna o tamanho atual do estoque delegando para o storage.
     */
    public int tamanho() {
        return storage.tamanho();
    }

    /**
     * Lista todas as bolsas no estoque delegando para o storage.
     */
    public Bolsa[] listarTodas() {
        return storage.listarTodas();
    }
}
