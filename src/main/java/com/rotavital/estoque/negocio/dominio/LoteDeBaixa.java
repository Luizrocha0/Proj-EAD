package com.rotavital.estoque.negocio.dominio;

import java.util.HashSet;
import java.util.List;

public final class LoteDeBaixa {
    private final List<OcorrenciaBolsa> ocorrencias;

    private LoteDeBaixa(List<OcorrenciaBolsa> ocorrencias) {
        this.ocorrencias = ocorrencias;
    }

    public static LoteDeBaixa confirmar(PedidoDeBaixa pedido, List<OcorrenciaBolsa> ocorrencias) {
        var confirmadas = List.copyOf(ocorrencias);
        if (confirmadas.size() != pedido.quantidade()) {
            throw new IllegalArgumentException("O lote deve conter a quantidade integral solicitada");
        }
        var chaves = new HashSet<Long>();
        for (var ocorrencia : confirmadas) {
            if (!chaves.add(ocorrencia.chave())) {
                throw new IllegalArgumentException("Cada ocorrência pode ser baixada uma única vez");
            }
            if (!ocorrencia.bolsa().elegivelPara(pedido.receptor(), pedido.referencia())) {
                throw new IllegalArgumentException("O lote só aceita bolsas compatíveis e válidas");
            }
        }
        return new LoteDeBaixa(confirmadas);
    }

    public List<OcorrenciaBolsa> ocorrencias() {
        return ocorrencias;
    }
}
