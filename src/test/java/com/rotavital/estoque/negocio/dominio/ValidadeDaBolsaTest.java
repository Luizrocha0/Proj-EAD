package com.rotavital.estoque.negocio.dominio;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class ValidadeDaBolsaTest {
    private static final LocalDate COLETA = LocalDate.of(2026, 11, 23);

    @Test
    void rejeitaValidadeAnteriorAColetaNoDominio() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> bolsaComValidade(COLETA.minusDays(1)))
                .withMessageContaining("validade");
    }

    @Test
    void aceitaBaixaNoUltimoDiaDeValidade() {
        var bolsa = bolsaComValidade(COLETA);

        assertThat(bolsa.isVencida(COLETA)).isFalse();
        assertThat(bolsa.isVencida(COLETA.plusDays(1))).isTrue();
    }

    private Bolsa bolsaComValidade(LocalDate validade) {
        return new Bolsa("V-1", "O-", 450, COLETA, validade, "Sintético");
    }
}
