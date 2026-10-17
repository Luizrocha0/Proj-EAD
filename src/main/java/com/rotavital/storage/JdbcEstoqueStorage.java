package com.rotavital.storage;

import com.rotavital.domain.Bolsa;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Profile("infra")
public class JdbcEstoqueStorage implements EstoqueStorage {

    private final JdbcTemplate jdbcTemplate;

    public JdbcEstoqueStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void salvar(Bolsa bolsa) {
        throw new UnsupportedOperationException("Stub para ciclo TDD Red T1.2");
    }

    @Override
    public Optional<Bolsa> buscarPorId(String id) {
        throw new UnsupportedOperationException("Stub para ciclo TDD Red T1.2");
    }

    @Override
    public boolean removerPorId(String id) {
        throw new UnsupportedOperationException("Pendente para Etapa 5 (H3)");
    }

    @Override
    public Bolsa[] listarTodas() {
        throw new UnsupportedOperationException("Pendente para Etapa 4 (H2)");
    }

    @Override
    public int tamanho() {
        throw new UnsupportedOperationException("Stub para ciclo TDD Red T1.2");
    }
}
