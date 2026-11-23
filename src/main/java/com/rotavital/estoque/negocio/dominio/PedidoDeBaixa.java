package com.rotavital.estoque.negocio.dominio;

import com.rotavital.comum.dominio.TipoSanguineo;

import java.time.LocalDate;
import java.util.Objects;

public record PedidoDeBaixa(TipoSanguineo receptor, int quantidade, LocalDate referencia) {
    public PedidoDeBaixa {
        Objects.requireNonNull(receptor);
        Objects.requireNonNull(referencia);
        if (quantidade <= 0) {
            throw new IllegalArgumentException("quantidadeBolsas deve ser positiva");
        }
    }
}
