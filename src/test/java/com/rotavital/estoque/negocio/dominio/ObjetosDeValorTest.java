package com.rotavital.estoque.negocio.dominio;

import com.rotavital.comum.dominio.TipoSanguineo;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class ObjetosDeValorTest {
    private static final LocalDate DATA = LocalDate.of(2026, 11, 23);

    @Test
    void validadeExigeAsDuasDatasEmOrdem() {
        assertThat(new Validade(DATA, DATA)).isEqualTo(new Validade(DATA, DATA));
        assertThatIllegalArgumentException().isThrownBy(() -> new Validade(DATA, DATA.minusDays(1)));
        assertThatNullPointerException().isThrownBy(() -> new Validade(null, DATA));
        assertThatNullPointerException().isThrownBy(() -> new Validade(DATA, null));
    }

    @Test
    void pedidoDeBaixaExigeReceptorQuantidadePositivaEReferencia() {
        assertThatIllegalArgumentException().isThrownBy(() -> new PedidoDeBaixa(TipoSanguineo.O_NEG, 0, DATA));
        assertThatNullPointerException().isThrownBy(() -> new PedidoDeBaixa(null, 1, DATA));
        assertThatNullPointerException().isThrownBy(() -> new PedidoDeBaixa(TipoSanguineo.O_NEG, 1, null));
        assertThat(new PedidoDeBaixa(TipoSanguineo.O_NEG, 1, DATA))
                .isEqualTo(new PedidoDeBaixa(TipoSanguineo.O_NEG, 1, DATA));
    }
}
