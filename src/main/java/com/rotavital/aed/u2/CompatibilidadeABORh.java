package com.rotavital.aed.u2;

import com.rotavital.domain.TipoSanguineo;

public final class CompatibilidadeABORh {
    // A ordem do enum coincide com a do C. Linhas são receptores e colunas são doadores.
    private static final boolean[][] MATRIZ = {
            {true,  false, false, false, false, false, false, false}, // O-
            {true,  true,  false, false, false, false, false, false}, // O+
            {true,  false, true,  false, false, false, false, false}, // A-
            {true,  true,  true,  true,  false, false, false, false}, // A+
            {true,  false, false, false, true,  false, false, false}, // B-
            {true,  true,  false, false, true,  true,  false, false}, // B+
            {true,  false, true,  false, true,  false, true,  false}, // AB-
            {true,  true,  true,  true,  true,  true,  true,  true}   // AB+
    };

    private CompatibilidadeABORh() {
    }

    public static boolean compativel(TipoSanguineo doador, TipoSanguineo receptor) {
        return MATRIZ[receptor.ordinal()][doador.ordinal()];
    }
}
