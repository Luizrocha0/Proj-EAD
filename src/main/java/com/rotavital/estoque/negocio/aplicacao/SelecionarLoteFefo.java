package com.rotavital.estoque.negocio.aplicacao;

import com.rotavital.aed.u2.AlocacaoFefo;
import com.rotavital.estoque.negocio.dominio.Bolsa;
import com.rotavital.estoque.negocio.dominio.LoteDeBaixa;
import com.rotavital.estoque.negocio.dominio.OcorrenciaBolsa;
import com.rotavital.estoque.negocio.dominio.PedidoDeBaixa;

import java.util.List;
import java.util.Optional;

public final class SelecionarLoteFefo {
    private SelecionarLoteFefo() {
    }

    public static Optional<LoteDeBaixa> selecionar(PedidoDeBaixa pedido, List<OcorrenciaBolsa> estoque) {
        var bolsas = estoque.stream().map(OcorrenciaBolsa::bolsa).toArray(Bolsa[]::new);
        var posicoes = AlocacaoFefo.selecionar(bolsas, pedido.receptor(), pedido.quantidade(), pedido.referencia());
        if (posicoes.isEmpty()) {
            return Optional.empty();
        }
        var ocorrencias = posicoes.stream().map(estoque::get).toList();
        return Optional.of(LoteDeBaixa.confirmar(pedido, ocorrencias));
    }
}
