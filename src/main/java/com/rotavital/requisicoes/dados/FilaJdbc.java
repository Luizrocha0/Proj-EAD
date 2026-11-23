package com.rotavital.requisicoes.dados;

import com.rotavital.comum.dominio.ConflitoAtendimentoException;
import com.rotavital.requisicoes.negocio.aplicacao.FilaDeRequisicoes;
import com.rotavital.requisicoes.negocio.dominio.Requisicao;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

@Repository
@Profile("infra")
public class FilaJdbc implements FilaDeRequisicoes {
    private static final String EM_ORDEM = """
            SELECT id, hospital_id, tipo_sanguineo, quantidade_bolsas, prioridade, data_hora
            FROM requisicoes ORDER BY ordem
            """;
    private static final RowMapper<Requisicao> PEDIDO = (rs, linha) -> new Requisicao(
            rs.getString("id"), rs.getString("hospital_id"), rs.getString("tipo_sanguineo"),
            rs.getInt("quantidade_bolsas"), Requisicao.Prioridade.valueOf(rs.getString("prioridade")),
            rs.getObject("data_hora", LocalDateTime.class));

    private final JdbcTemplate jdbc;

    public FilaJdbc(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
    public int enfileirar(Requisicao requisicao) {
        bloquearEscritasNaFila();
        int inseridas = jdbc.update("""
                INSERT INTO requisicoes (id, hospital_id, tipo_sanguineo, quantidade_bolsas, prioridade, data_hora)
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT (id) DO NOTHING
                """, requisicao.getId(), requisicao.getHospitalId(), requisicao.getTipoSanguineo(),
                requisicao.getQuantidadeBolsas(), requisicao.getPrioridade().name(), requisicao.getDataHora());
        if (inseridas != 1) {
            throw new ConflitoAtendimentoException("Já existe uma requisição pendente com esse ID");
        }
        return tamanho();
    }

    @Override
    public Optional<Requisicao> proxima() {
        return jdbc.query(EM_ORDEM + "LIMIT 1", PEDIDO).stream().findFirst();
    }

    @Override
    public List<Requisicao> listar() {
        return jdbc.query(EM_ORDEM, PEDIDO);
    }

    @Override
    @Transactional
    public <T> Optional<Consumo<T>> consumirProxima(Function<Requisicao, T> atendimento) {
        bloquearEscritasNaFila();
        return proxima().map(requisicao -> {
            T resultado = Objects.requireNonNull(atendimento.apply(requisicao));
            if (jdbc.update("DELETE FROM requisicoes WHERE id = ?", requisicao.getId()) != 1) {
                throw new ConflitoAtendimentoException("A requisição mudou durante o atendimento. Tente novamente");
            }
            return new Consumo<>(resultado, tamanho());
        });
    }

    private void bloquearEscritasNaFila() {
        jdbc.execute("LOCK TABLE requisicoes IN SHARE ROW EXCLUSIVE MODE");
    }

    private int tamanho() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM requisicoes", Integer.class);
    }
}
