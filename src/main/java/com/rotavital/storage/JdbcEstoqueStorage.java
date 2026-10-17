package com.rotavital.storage;

import com.rotavital.domain.Bolsa;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.util.List;
import java.util.Optional;

@Component
@Profile("infra")
public class JdbcEstoqueStorage implements EstoqueStorage {

    private final JdbcTemplate jdbcTemplate;

    public JdbcEstoqueStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = java.util.Objects.requireNonNull(jdbcTemplate, "jdbcTemplate não pode ser nulo");
    }

    @Override
    @Transactional
    public void salvar(Bolsa bolsa) {
        java.util.Objects.requireNonNull(bolsa, "bolsa não pode ser nula");
        String sql = """
            INSERT INTO bolsas (id, tipo_sanguineo, volume_ml, data_coleta, validade, doador)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
        jdbcTemplate.update(sql,
                bolsa.getId(),
                bolsa.getTipoSanguineo(),
                bolsa.getVolumeMl(),
                Date.valueOf(bolsa.getDataColeta()),
                Date.valueOf(bolsa.getValidade()),
                bolsa.getDoador()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Bolsa> buscarPorId(String id) {
        if (id == null || id.isBlank() || id.trim().length() > 50) {
            return Optional.empty();
        }
        String sql = """
            SELECT id, tipo_sanguineo, volume_ml, data_coleta, validade, doador
            FROM bolsas
            WHERE id = ?
            ORDER BY id_seq DESC
            LIMIT 1
            """;
        List<Bolsa> resultados = jdbcTemplate.query(sql, bolsaRowMapper, id.trim());
        return resultados.isEmpty() ? Optional.empty() : Optional.of(resultados.get(0));
    }

    @Override
    @Transactional
    public boolean removerPorId(String id) {
        throw new UnsupportedOperationException("Pendente para Etapa 5 (H3)");
    }

    @Override
    @Transactional(readOnly = true)
    public List<Bolsa> listar() {
        throw new UnsupportedOperationException("Pendente para Etapa 4 (H2)");
    }

    @Override
    @Transactional(readOnly = true)
    public Bolsa[] listarTodas() {
        throw new UnsupportedOperationException("Pendente para Etapa 4 (H2)");
    }

    @Override
    @Transactional(readOnly = true)
    public int tamanho() {
        Integer total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM bolsas", Integer.class);
        return total != null ? total : 0;
    }

    private final RowMapper<Bolsa> bolsaRowMapper = (rs, rowNum) -> {
        Date dataColetaSql = rs.getDate("data_coleta");
        Date validadeSql = rs.getDate("validade");
        return new Bolsa(
                rs.getString("id"),
                rs.getString("tipo_sanguineo"),
                rs.getDouble("volume_ml"),
                dataColetaSql != null ? dataColetaSql.toLocalDate() : null,
                validadeSql != null ? validadeSql.toLocalDate() : null,
                rs.getString("doador")
        );
    };
}
