package com.rotavital.integration;

import com.rotavital.comum.dominio.TipoSanguineo;
import com.rotavital.estoque.dados.JdbcEstoqueStorage;
import com.rotavital.estoque.negocio.dominio.Bolsa;
import com.rotavital.estoque.negocio.dominio.PedidoDeBaixa;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class JdbcEstoqueContratoIT extends AbstractPostgresIT {
    private static final LocalDate REFERENCIA = LocalDate.of(2026, 11, 23);

    @Autowired
    private JdbcEstoqueStorage estoque;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void prepararEstoque() {
        jdbc.update("DELETE FROM bolsas");
    }

    @Test
    void consultaERemoveIdNoLimiteComEspacos() {
        String id = "A".repeat(50);
        estoque.salvar(bolsa(id, "O-"));

        assertThat(estoque.buscarPorId("  " + id + "  ")).isPresent();
        assertThat(estoque.removerPorId("  " + id + "  ")).isTrue();
        assertThat(estoque.tamanho()).isZero();
    }

    @Test
    void preservaRotuloLegadoNoEstoqueMasNaoUsaNaBaixa() {
        estoque.salvar(bolsa("LEGADO", "LEGADO"));
        estoque.salvar(bolsa("COMPATIVEL", "O-"));

        var lote = estoque.alocar(new PedidoDeBaixa(TipoSanguineo.O_NEG, 1, REFERENCIA)).orElseThrow();

        assertThat(lote.ocorrencias()).extracting(ocorrencia -> ocorrencia.bolsa().getId())
                .containsExactly("COMPATIVEL");
        assertThat(estoque.buscarPorId("LEGADO")).get().extracting(Bolsa::getTipoSanguineo).isEqualTo("LEGADO");
    }

    @Test
    void propagaFalhaDoPostgresqlSemGravarEmMemoria() {
        assertThrows(DataIntegrityViolationException.class, () -> estoque.salvar(bolsa("A".repeat(51), "O-")));
        assertThat(estoque.tamanho()).isZero();
    }

    private Bolsa bolsa(String id, String tipo) {
        return new Bolsa(id, tipo, 450, REFERENCIA.minusDays(5), REFERENCIA, "Sintético");
    }
}
