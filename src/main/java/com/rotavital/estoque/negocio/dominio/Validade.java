package com.rotavital.estoque.negocio.dominio;

import java.time.LocalDate;
import java.util.Objects;

public record Validade(LocalDate coleta, LocalDate vencimento) {
    public Validade {
        Objects.requireNonNull(coleta, "dataColeta não pode ser nula");
        Objects.requireNonNull(vencimento, "validade não pode ser nula");
        if (vencimento.isBefore(coleta)) {
            throw new IllegalArgumentException("validade não pode ser anterior à data de coleta");
        }
    }

    public boolean vencidaEm(LocalDate referencia) {
        return vencimento.isBefore(referencia);
    }
}
