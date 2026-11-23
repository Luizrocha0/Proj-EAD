package com.rotavital.integration;

import com.rotavital.estoque.negocio.aplicacao.EstoqueService;
import com.rotavital.estoque.negocio.aplicacao.EstoqueStorage;
import com.rotavital.estoque.dados.JdbcEstoqueStorage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

@SpringBootTest
@DisplayName("Verificação de Injeção de Dependência do Spring - Perfil infra")
class EstoqueServiceInfraProfileInjectionIT extends AbstractPostgresIT {

    @Autowired
    private EstoqueService service;

    @Autowired
    private EstoqueStorage storage;

    @Test
    @DisplayName("Sob perfil infra, Spring deve injetar JdbcEstoqueStorage em EstoqueService")
    void deveInjetarJdbcEstoqueStorageSobPerfilInfra() {
        assertInstanceOf(JdbcEstoqueStorage.class, storage, "Storage ativo deve ser JdbcEstoqueStorage");
        assertSame(storage, service.getStorage(), "EstoqueService deve ter recebido o bean JdbcEstoqueStorage do Spring");
    }
}
