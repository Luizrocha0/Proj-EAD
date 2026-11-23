package com.rotavital.expedicao.dados;

import com.rotavital.comum.dominio.TipoSanguineo;
import com.rotavital.estoque.negocio.aplicacao.EstoqueService;
import com.rotavital.estoque.negocio.dominio.PedidoDeBaixa;
import com.rotavital.expedicao.negocio.aplicacao.BaixaParaExpedicao;
import com.rotavital.expedicao.negocio.dominio.GuiaDeSaida;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
public class AdaptadorEstoque implements BaixaParaExpedicao {
    private final EstoqueService estoque;

    public AdaptadorEstoque(EstoqueService estoque) {
        this.estoque = estoque;
    }

    @Override
    public Optional<List<GuiaDeSaida.Item>> baixar(TipoSanguineo receptor, int quantidade, LocalDate data) {
        return estoque.alocar(new PedidoDeBaixa(receptor, quantidade, data))
                .map(lote -> lote.ocorrencias().stream().map(ocorrencia -> {
                    var bolsa = ocorrencia.bolsa();
                    return new GuiaDeSaida.Item(ocorrencia.chave(), bolsa.getId(), bolsa.getTipoSanguineo(),
                            bolsa.getVolumeMl(), bolsa.getValidade());
                }).toList());
    }
}
