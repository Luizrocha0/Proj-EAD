package com.rotavital.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;

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
    @ConditionalOnExpression("!'${DATABASE_URL:}'.trim().isEmpty()")
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

        String trimmed = dbUrl.trim();
        String lowerTrimmed = trimmed.toLowerCase();

        if (lowerTrimmed.startsWith("jdbc:postgresql:") || lowerTrimmed.startsWith("jdbc:postgres:")) {
            return DataSourceBuilder.create()
                    .url(trimmed)
                    .driverClassName("org.postgresql.Driver")
                    .build();
        }

        if (lowerTrimmed.startsWith("jdbc:")) {
            throw new IllegalArgumentException("Esquema JDBC não suportado na DATABASE_URL (esperado 'jdbc:postgresql:'): " + dbUrl);
        }

        if (!lowerTrimmed.startsWith("postgres://") && !lowerTrimmed.startsWith("postgresql://")) {
            throw new IllegalArgumentException("Esquema de URL não suportado na DATABASE_URL (esperado 'postgres://', 'postgresql://' ou 'jdbc:postgresql://'): " + dbUrl);
        }

        try {
            // Remove prefixos postgres:// ou postgresql:// (case-insensitive)
            String withoutScheme = trimmed.replaceFirst("^(?i)postgres(?:ql)?://", "");

            // Remover fragmento URI (#...) se presente antes do processamento de query e path
            int fragmentIndex = withoutScheme.indexOf('#');
            if (fragmentIndex >= 0) {
                withoutScheme = withoutScheme.substring(0, fragmentIndex);
            }

            // Separar query string (?...) se houver
            String query = null;
            int queryIndex = withoutScheme.indexOf('?');
            if (queryIndex >= 0) {
                query = withoutScheme.substring(queryIndex + 1);
                withoutScheme = withoutScheme.substring(0, queryIndex);
            }

            // Separar caminho do banco (/...) da autoridade (userInfo@host:port)
            String path = "/rotavital";
            int slashIndex = withoutScheme.indexOf('/');
            String authority = withoutScheme;
            if (slashIndex >= 0) {
                authority = withoutScheme.substring(0, slashIndex);
                String subPath = withoutScheme.substring(slashIndex);
                String cleanPath = subPath.replaceAll("^/+", "/").replaceAll("/+$", "");
                if (!cleanPath.isBlank() && !cleanPath.equals("/")) {
                    path = cleanPath;
                }
            }

            // Separar userInfo (username:password) do host:port
            // Usa o último '@' para permitir senhas com '@' ou outros caracteres especiais
            String username = null;
            String password = null;
            String hostPort = authority;
            int atIndex = authority.lastIndexOf('@');
            if (atIndex >= 0) {
                String userInfo = authority.substring(0, atIndex);
                hostPort = authority.substring(atIndex + 1);

                int colonIndex = userInfo.indexOf(':');
                if (colonIndex >= 0) {
                    username = decode(userInfo.substring(0, colonIndex));
                    password = decode(userInfo.substring(colonIndex + 1));
                } else {
                    username = decode(userInfo);
                }
            }

            // Separar host e porta (com suporte a hostnames com underscore e IPv6)
            String host;
            int port = 5432;
            if (hostPort.startsWith("[")) {
                int closingBracket = hostPort.indexOf(']');
                if (closingBracket <= 0) {
                    throw new IllegalArgumentException("Endereço IPv6 com colchetes inválidos na DATABASE_URL: " + dbUrl);
                }
                host = hostPort.substring(0, closingBracket + 1);
                if (closingBracket + 1 < hostPort.length()) {
                    if (hostPort.charAt(closingBracket + 1) == ':') {
                        String portStr = hostPort.substring(closingBracket + 2);
                        if (!portStr.isBlank()) {
                            port = parsePort(portStr, dbUrl);
                        }
                    } else {
                        throw new IllegalArgumentException("Formato inválido após colchetes de IPv6 na DATABASE_URL (esperado ':porta' ou fim da string): " + dbUrl);
                    }
                }
            } else {
                int lastColon = hostPort.lastIndexOf(':');
                if (lastColon >= 0) {
                    host = hostPort.substring(0, lastColon);
                    String portStr = hostPort.substring(lastColon + 1);
                    if (!portStr.isBlank()) {
                        port = parsePort(portStr, dbUrl);
                    }
                } else {
                    host = hostPort;
                }
            }

            if (host == null || host.isBlank()) {
                throw new IllegalArgumentException("Host não especificado na DATABASE_URL: " + dbUrl);
            }

            String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + path;
            if (query != null && !query.isBlank()) {
                jdbcUrl += "?" + query;
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
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Erro ao processar DATABASE_URL: " + dbUrl, e);
        }
    }

    private static int parsePort(String portStr, String dbUrl) {
        try {
            int port = Integer.parseInt(portStr.trim());
            if (port < 1 || port > 65535) {
                throw new IllegalArgumentException("Porta fora do intervalo permitido (1-65535) na DATABASE_URL: " + dbUrl);
            }
            return port;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Porta numérica inválida na DATABASE_URL: " + dbUrl, e);
        }
    }

    private static String decode(String value) {
        if (value == null) {
            return null;
        }
        // Preserva '+' literal convertendo para %2B antes do decode standard
        String safe = value.replace("+", "%2B");
        return java.net.URLDecoder.decode(safe, java.nio.charset.StandardCharsets.UTF_8);
    }
}
