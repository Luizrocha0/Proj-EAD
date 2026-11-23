package com.rotavital.comum.dominio;

import java.util.Optional;

public enum TipoSanguineo {
    O_NEG("O-"), O_POS("O+"), A_NEG("A-"), A_POS("A+"),
    B_NEG("B-"), B_POS("B+"), AB_NEG("AB-"), AB_POS("AB+");

    private static final TipoSanguineo[] TIPOS = values();
    private static final boolean[][] COMPATIBILIDADE = {
            {true, false, false, false, false, false, false, false}, // O-
            {true, true,  false, false, false, false, false, false}, // O+
            {true, false, true,  false, false, false, false, false}, // A-
            {true, true,  true,  true,  false, false, false, false}, // A+
            {true, false, false, false, true,  false, false, false}, // B-
            {true, true,  false, false, true,  true,  false, false}, // B+
            {true, false, true,  false, true,  false, true,  false}, // AB-
            {true, true,  true,  true,  true,  true,  true,  true}   // AB+
    };

    private final String rotulo;

    TipoSanguineo(String rotulo) {
        this.rotulo = rotulo;
    }

    public String rotulo() {
        return rotulo;
    }

    public boolean aceitaDoacaoDe(TipoSanguineo doador) {
        return COMPATIBILIDADE[ordinal()][doador.ordinal()];
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
