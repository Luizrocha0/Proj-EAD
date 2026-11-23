package com.rotavital.aed.u2;

import com.rotavital.comum.dominio.TipoSanguineo;

public final class CompatibilidadeABORh {
    private CompatibilidadeABORh() {
    }

    public static boolean compativel(TipoSanguineo doador, TipoSanguineo receptor) {
        return receptor.aceitaDoacaoDe(doador);
    }
}
