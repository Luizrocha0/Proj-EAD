package com.rotavital.service;

import com.rotavital.storage.EstoqueStorage;
import com.rotavital.storage.JdbcEstoqueStorage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

@SpringBootTest
@ActiveProfiles("infra")
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "management.health.db.enabled=false",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration"
})
@DisplayName("Verificação de Injeção de Dependência do Spring - Perfil infra")
class EstoqueServiceInfraProfileInjectionTest {

    @MockBean
    private JdbcTemplate jdbcTemplate;

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
