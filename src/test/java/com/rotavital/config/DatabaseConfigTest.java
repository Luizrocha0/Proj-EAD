package com.rotavital.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Config — Suporte a DATABASE_URL no Perfil Infra")
class DatabaseConfigTest {

    private final DatabaseConfig config = new DatabaseConfig();

    @Test
    @DisplayName("Deve converter URL no formato postgres:// para JDBC DataSource")
    void deveConverterUrlPostgresParaJdbc() {
        String url = "postgres://rotavital:senha123@dpg-srv-1:5432/rotavital_prod";
        DataSource ds = DatabaseConfig.parseAndBuildDataSource(url);

        assertNotNull(ds);
    }

    @Test
    @DisplayName("Deve converter URL no formato postgresql:// para JDBC DataSource")
    void deveConverterUrlPostgresqlParaJdbc() {
        String url = "postgresql://rotavital:senha123@dpg-srv-2:5432/rotavital_db";
        DataSource ds = DatabaseConfig.parseAndBuildDataSource(url);

        assertNotNull(ds);
    }

    @Test
    @DisplayName("Deve aceitar URL já no formato JDBC diretamente")
    void deveAceitarUrlJdbcDiretamente() {
        String url = "jdbc:postgresql://localhost:5432/rotavital";
        DataSource ds = DatabaseConfig.parseAndBuildDataSource(url);

        assertNotNull(ds);
    }

    @Test
    @DisplayName("Deve preservar parâmetros query na conversão")
    void devePreservarParametrosQuery() {
        String url = "postgres://usr:pwd@host:5432/db?sslmode=require";
        DataSource ds = DatabaseConfig.parseAndBuildDataSource(url);

        assertNotNull(ds);
    }

    @Test
    @DisplayName("Deve lançar exceção para URL nula ou vazia")
    void deveLancarExcecaoParaUrlInvalida() {
        assertThrows(IllegalArgumentException.class, () -> DatabaseConfig.parseAndBuildDataSource(null));
        assertThrows(IllegalArgumentException.class, () -> DatabaseConfig.parseAndBuildDataSource(""));
        assertThrows(IllegalArgumentException.class, () -> DatabaseConfig.parseAndBuildDataSource("   "));
    }

    @Test
    @DisplayName("Deve instanciar bean via MockEnvironment quando DATABASE_URL estiver presente")
    void deveInstanciarBeanViaEnvironment() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("DATABASE_URL", "postgresql://user:pass@host:5432/mydb");

        DataSource ds = config.dataSourceFromDatabaseUrl(env);
        assertNotNull(ds);
    }

    @Test
    @DisplayName("Deve retornar null via MockEnvironment quando DATABASE_URL for vazia")
    void deveRetornarNullQuandoDatabaseUrlVazia() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("DATABASE_URL", "");

        DataSource ds = config.dataSourceFromDatabaseUrl(env);
        assertNull(ds);
    }
}
