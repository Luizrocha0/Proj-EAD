package com.rotavital.estoque.negocio.dominio;

import com.rotavital.comum.dominio.TipoSanguineo;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LoteDeBaixaTest {
    private static final LocalDate HOJE = LocalDate.of(2026, 11, 23);
    private static final PedidoDeBaixa PEDIDO = new PedidoDeBaixa(TipoSanguineo.A_NEG, 2, HOJE);

    @Test
    void confirmaOcorrenciasDistintasComMesmoIdPublicoNaDataDeValidade() {
        var primeira = ocorrencia(1, "O-", HOJE);
        var segunda = ocorrencia(2, "A-", HOJE.plusDays(1));
        var ocorrencias = new ArrayList<>(List.of(primeira, segunda));

        var lote = LoteDeBaixa.confirmar(PEDIDO, ocorrencias);
        ocorrencias.clear();

        assertThat(lote.ocorrencias()).containsExactly(primeira, segunda);
        assertThrows(UnsupportedOperationException.class, () -> lote.ocorrencias().clear());
    }

    @Test
    void rejeitaQuantidadeParcialOuExcedente() {
        var primeira = ocorrencia(1, "O-", HOJE);
        var segunda = ocorrencia(2, "O-", HOJE);
        var terceira = ocorrencia(3, "O-", HOJE);

        assertThrows(IllegalArgumentException.class, () -> LoteDeBaixa.confirmar(PEDIDO, List.of(primeira)));
        assertThrows(IllegalArgumentException.class,
                () -> LoteDeBaixa.confirmar(PEDIDO, List.of(primeira, segunda, terceira)));
    }

    @Test
    void rejeitaOcorrenciaRepetidaVencidaOuIncompativel() {
        var primeira = ocorrencia(1, "O-", HOJE);

        assertThrows(IllegalArgumentException.class,
                () -> LoteDeBaixa.confirmar(PEDIDO, List.of(primeira, primeira)));
        assertThrows(IllegalArgumentException.class,
                () -> LoteDeBaixa.confirmar(PEDIDO, List.of(primeira, ocorrencia(2, "A-", HOJE.minusDays(1)))));
        assertThrows(IllegalArgumentException.class,
                () -> LoteDeBaixa.confirmar(PEDIDO, List.of(primeira, ocorrencia(2, "A+", HOJE))));
    }

    private OcorrenciaBolsa ocorrencia(long chave, String tipo, LocalDate validade) {
        return new OcorrenciaBolsa(chave, new Bolsa("ID-REPETIDO", tipo, 450,
                HOJE.minusDays(5), validade, "Sintético"));
    }
}
