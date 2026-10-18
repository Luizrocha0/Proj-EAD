package com.rotavital.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.net.URI;

/**
 * Configuração de DataSource para o perfil 'infra' quando a variável de ambiente
 * DATABASE_URL for fornecida (como no Render Blueprint e ambientes PaaS).
 * Converte URIs dos formatos 'postgres://' e 'postgresql://' em JDBC URLs compatíveis.
 */
@Configuration
@Profile("infra")
public class DatabaseConfig {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConfig.class);

    @Bean
    @Primary
    @ConditionalOnProperty(name = "DATABASE_URL")
    public DataSource dataSourceFromDatabaseUrl(Environment env) {
        String dbUrl = env.getProperty("DATABASE_URL");
        if (dbUrl == null || dbUrl.isBlank()) {
            return null;
        }
        log.info("Inicializando DataSource a partir de DATABASE_URL (Render/PaaS)");
        return parseAndBuildDataSource(dbUrl);
    }

    public static DataSource parseAndBuildDataSource(String dbUrl) {
        if (dbUrl == null || dbUrl.isBlank()) {
            throw new IllegalArgumentException("DATABASE_URL não pode ser nulo ou vazio");
        }

        if (dbUrl.startsWith("jdbc:")) {
            return DataSourceBuilder.create()
                    .url(dbUrl)
                    .driverClassName("org.postgresql.Driver")
                    .build();
        }

        try {
            // Suporta formatos postgres:// e postgresql:// do Render/Heroku
            String sanitized = dbUrl.replaceFirst("^postgres(ql)?://", "http://");
            URI uri = URI.create(sanitized);

            String host = uri.getHost();
            int port = uri.getPort() > 0 ? uri.getPort() : 5432;
            String path = uri.getPath();
            if (path == null || path.isBlank() || path.equals("/")) {
                path = "/rotavital";
            }

            String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + path;
            if (uri.getQuery() != null && !uri.getQuery().isBlank()) {
                jdbcUrl += "?" + uri.getQuery();
            }

            String username = null;
            String password = null;
            if (uri.getUserInfo() != null) {
                String[] parts = uri.getUserInfo().split(":", 2);
                username = parts[0];
                if (parts.length > 1) {
                    password = parts[1];
                }
            }

            var builder = DataSourceBuilder.create()
                    .url(jdbcUrl)
                    .driverClassName("org.postgresql.Driver");

            if (username != null && !username.isBlank()) {
                builder.username(username);
            }
            if (password != null && !password.isBlank()) {
                builder.password(password);
            }

            return builder.build();
        } catch (Exception e) {
            throw new IllegalArgumentException("Erro ao processar DATABASE_URL: " + dbUrl, e);
        }
    }
}
