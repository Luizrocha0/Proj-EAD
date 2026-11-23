package com.rotavital.estoque.negocio.dominio;

import java.util.Objects;

public record OcorrenciaBolsa(long chave, Bolsa bolsa) {
    public OcorrenciaBolsa {
        Objects.requireNonNull(bolsa);
        if (chave <= 0) {
            throw new IllegalArgumentException("A ocorrência deve ter uma chave positiva");
        }
    }
}
