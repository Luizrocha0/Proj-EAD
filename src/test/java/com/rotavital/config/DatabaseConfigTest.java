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
        com.zaxxer.hikari.HikariDataSource hds = (com.zaxxer.hikari.HikariDataSource) ds;
        assertEquals("jdbc:postgresql://dpg-srv-1:5432/rotavital_prod", hds.getJdbcUrl());
        assertEquals("rotavital", hds.getUsername());
        assertEquals("senha123", hds.getPassword());
    }

    @Test
    @DisplayName("Deve suportar host com underscore ou caracteres especiais na senha")
    void deveSuportarHostComUnderscoreESenhaComCaracteres() {
        String url = "postgres://rotavital:p%40ss%23123@dpg_srv_1:5432/rotavital_prod";
        DataSource ds = DatabaseConfig.parseAndBuildDataSource(url);

        assertNotNull(ds);
        com.zaxxer.hikari.HikariDataSource hds = (com.zaxxer.hikari.HikariDataSource) ds;
        assertEquals("jdbc:postgresql://dpg_srv_1:5432/rotavital_prod", hds.getJdbcUrl());
        assertEquals("rotavital", hds.getUsername());
        assertEquals("p@ss#123", hds.getPassword());
    }

    @Test
    @DisplayName("Deve converter URL no formato postgresql:// para JDBC DataSource")
    void deveConverterUrlPostgresqlParaJdbc() {
        String url = "postgresql://rotavital:senha123@dpg-srv-2:5432/rotavital_db";
        DataSource ds = DatabaseConfig.parseAndBuildDataSource(url);

        assertNotNull(ds);
        com.zaxxer.hikari.HikariDataSource hds = (com.zaxxer.hikari.HikariDataSource) ds;
        assertEquals("jdbc:postgresql://dpg-srv-2:5432/rotavital_db", hds.getJdbcUrl());
        assertEquals("rotavital", hds.getUsername());
        assertEquals("senha123", hds.getPassword());
    }

    @Test
    @DisplayName("Deve aceitar URL já no formato JDBC diretamente")
    void deveAceitarUrlJdbcDiretamente() {
        String url = "jdbc:postgresql://localhost:5432/rotavital";
        DataSource ds = DatabaseConfig.parseAndBuildDataSource(url);

        assertNotNull(ds);
        com.zaxxer.hikari.HikariDataSource hds = (com.zaxxer.hikari.HikariDataSource) ds;
        assertEquals("jdbc:postgresql://localhost:5432/rotavital", hds.getJdbcUrl());
    }

    @Test
    @DisplayName("Deve preservar parâmetros query na conversão")
    void devePreservarParametrosQuery() {
        String url = "postgres://usr:pwd@host:5432/db?sslmode=require&connectTimeout=10";
        DataSource ds = DatabaseConfig.parseAndBuildDataSource(url);

        assertNotNull(ds);
        com.zaxxer.hikari.HikariDataSource hds = (com.zaxxer.hikari.HikariDataSource) ds;
        assertEquals("jdbc:postgresql://host:5432/db?sslmode=require&connectTimeout=10", hds.getJdbcUrl());
        assertEquals("usr", hds.getUsername());
        assertEquals("pwd", hds.getPassword());
    }

    @Test
    @DisplayName("Deve aplicar porta padrão 5432 e banco padrão /rotavital quando omitidos")
    void deveAplicarPortaEBancoPadrao() {
        String url = "postgres://user:pass@render-db-host";
        DataSource ds = DatabaseConfig.parseAndBuildDataSource(url);

        assertNotNull(ds);
        com.zaxxer.hikari.HikariDataSource hds = (com.zaxxer.hikari.HikariDataSource) ds;
        assertEquals("jdbc:postgresql://render-db-host:5432/rotavital", hds.getJdbcUrl());
        assertEquals("user", hds.getUsername());
        assertEquals("pass", hds.getPassword());
    }

    @Test
    @DisplayName("Deve suportar literal + e IPv6")
    void deveSuportarLiteralPlusEIPv6() {
        String url = "postgres://rotavital:segredo+123@[::1]:5433/rotavital";
        DataSource ds = DatabaseConfig.parseAndBuildDataSource(url);

        assertNotNull(ds);
        com.zaxxer.hikari.HikariDataSource hds = (com.zaxxer.hikari.HikariDataSource) ds;
        assertEquals("jdbc:postgresql://[::1]:5433/rotavital", hds.getJdbcUrl());
        assertEquals("rotavital", hds.getUsername());
        assertEquals("segredo+123", hds.getPassword());
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

    @Test
    @DisplayName("Deve lançar exceção para esquemas não suportados (ex: http, mysql, redis)")
    void deveLancarExcecaoParaEsquemasNaoSuportados() {
        assertThrows(IllegalArgumentException.class, () -> DatabaseConfig.parseAndBuildDataSource("http://localhost:5432/rotavital"));
        assertThrows(IllegalArgumentException.class, () -> DatabaseConfig.parseAndBuildDataSource("mysql://root:secret@localhost:3306/db"));
        assertThrows(IllegalArgumentException.class, () -> DatabaseConfig.parseAndBuildDataSource("redis://localhost:6379"));
    }

    @Test
    @DisplayName("Deve suportar esquemas em maiúsculas ou mistos (RFC 3986)")
    void deveSuportarEsquemaEmMaiusculas() {
        String urlPostgresUpper = "POSTGRES://rotavital:senha123@dpg-srv-1:5432/rotavital_prod";
        DataSource ds1 = DatabaseConfig.parseAndBuildDataSource(urlPostgresUpper);
        assertNotNull(ds1);
        com.zaxxer.hikari.HikariDataSource hds1 = (com.zaxxer.hikari.HikariDataSource) ds1;
        assertEquals("jdbc:postgresql://dpg-srv-1:5432/rotavital_prod", hds1.getJdbcUrl());
        assertEquals("rotavital", hds1.getUsername());

        String urlPostgresqlUpper = "PostgreSQL://rotavital:senha123@dpg-srv-2:5432/rotavital_db";
        DataSource ds2 = DatabaseConfig.parseAndBuildDataSource(urlPostgresqlUpper);
        assertNotNull(ds2);
        com.zaxxer.hikari.HikariDataSource hds2 = (com.zaxxer.hikari.HikariDataSource) ds2;
        assertEquals("jdbc:postgresql://dpg-srv-2:5432/rotavital_db", hds2.getJdbcUrl());

        String urlJdbcUpper = "JDBC:postgresql://localhost:5432/rotavital";
        DataSource ds3 = DatabaseConfig.parseAndBuildDataSource(urlJdbcUpper);
        assertNotNull(ds3);
        com.zaxxer.hikari.HikariDataSource hds3 = (com.zaxxer.hikari.HikariDataSource) ds3;
        assertEquals("JDBC:postgresql://localhost:5432/rotavital", hds3.getJdbcUrl());
    }

    @Test
    @DisplayName("Deve suportar IPv6 com dois pontos sem porta definida caindo na porta padrão 5432")
    void deveSuportarIpv6ComDoisPontosSemPorta() {
        String url = "postgres://rotavital:pass@[::1]:/rotavital";
        DataSource ds = DatabaseConfig.parseAndBuildDataSource(url);

        assertNotNull(ds);
        com.zaxxer.hikari.HikariDataSource hds = (com.zaxxer.hikari.HikariDataSource) ds;
        assertEquals("jdbc:postgresql://[::1]:5432/rotavital", hds.getJdbcUrl());
        assertEquals("rotavital", hds.getUsername());
        assertEquals("pass", hds.getPassword());
    }

    @Test
    @DisplayName("Deve lançar exceção para IPv6 com colchete não fechado")
    void deveLancarExcecaoParaIpv6SemFechamentoDeColchete() {
        String url = "postgres://rotavital:pass@[::1/rotavital";
        assertThrows(IllegalArgumentException.class, () -> DatabaseConfig.parseAndBuildDataSource(url));
    }
}
