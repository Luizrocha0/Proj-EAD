package com.rotavital.expedicao.negocio.dominio;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GuiaDeSaidaTest {
    private final GuiaDeSaida.Item primeira = item(1);
    private final GuiaDeSaida.Item segunda = item(2);

    @Test
    void identificaPedidoHospitalEOcorrenciasMesmoComIdPublicoRepetido() {
        var guia = GuiaDeSaida.emitir("REQ-1", "HOSP-1", 2, List.of(primeira, segunda));

        assertThat(guia.requisicaoId()).isEqualTo("REQ-1");
        assertThat(guia.hospitalId()).isEqualTo("HOSP-1");
        assertThat(guia.itens()).containsExactly(primeira, segunda);
    }

    @Test
    void exigeQuantidadeIntegralEOcorrenciasDistintas() {
        assertThrows(IllegalArgumentException.class,
                () -> GuiaDeSaida.emitir("REQ-1", "HOSP-1", 0, List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> GuiaDeSaida.emitir("REQ-1", "HOSP-1", 2, List.of(primeira)));
        assertThrows(IllegalArgumentException.class,
                () -> GuiaDeSaida.emitir("REQ-1", "HOSP-1", 2, List.of(primeira, primeira)));
    }

    private GuiaDeSaida.Item item(long chave) {
        return new GuiaDeSaida.Item(chave, "BOLSA-DUP", "O-", 450, LocalDate.of(2026, 11, 23));
    }
}
