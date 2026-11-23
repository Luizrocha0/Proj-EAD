package com.rotavital.domain;

import java.util.Optional;

public enum TipoSanguineo {
    O_NEG("O-"), O_POS("O+"), A_NEG("A-"), A_POS("A+"),
    B_NEG("B-"), B_POS("B+"), AB_NEG("AB-"), AB_POS("AB+");

    private static final TipoSanguineo[] TIPOS = values();

    private final String rotulo;

    TipoSanguineo(String rotulo) {
        this.rotulo = rotulo;
    }

    public String rotulo() {
        return rotulo;
    }

    public static Optional<TipoSanguineo> identificar(String rotulo) {
        for (TipoSanguineo tipo : TIPOS) {
            if (tipo.rotulo.equals(rotulo)) {
                return Optional.of(tipo);
            }
        }
        return Optional.empty();
    }

    public static TipoSanguineo exigir(String rotulo) {
        return identificar(rotulo)
                .orElseThrow(() -> new IllegalArgumentException("tipoSanguineo inválido"));
    }
}
