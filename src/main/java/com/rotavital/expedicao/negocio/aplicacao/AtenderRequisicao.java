package com.rotavital.expedicao.negocio.aplicacao;

import com.rotavital.comum.dominio.ConflitoAtendimentoException;
import com.rotavital.expedicao.negocio.dominio.GuiaDeSaida;
import com.rotavital.expedicao.negocio.dominio.Roteirizador;
import com.rotavital.requisicoes.negocio.aplicacao.RequisicaoService;
import com.rotavital.requisicoes.negocio.dominio.Requisicao;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class AtenderRequisicao {
    private final RequisicaoService requisicoes;
    private final BaixaParaExpedicao estoque;
    private final Clock relogio;

    public AtenderRequisicao(RequisicaoService requisicoes, BaixaParaExpedicao estoque, Clock relogio) {
        this.requisicoes = requisicoes;
        this.estoque = estoque;
        this.relogio = relogio;
    }

    public Optional<Atendimento> executar(String requisicaoId, Roteirizador.Ponto origem,
                                          List<Roteirizador.Ponto> paradas, Roteirizador.Ponto destino) {
        return requisicoes.atenderProxima(requisicao -> {
            requisicao.conferirAtendimento(requisicaoId, destino.id());
            var rota = Roteirizador.calcular(origem, paradas, destino);
            int quantidade = requisicao.getQuantidadeBolsas();
            var itens = estoque.baixar(requisicao.tipoSolicitado(), quantidade, LocalDate.now(relogio))
                    .orElseThrow(() -> new ConflitoAtendimentoException(
                            "Estoque compatível e válido insuficiente para o pedido"));
            var guia = GuiaDeSaida.emitir(requisicao.getId(), requisicao.getHospitalId(), quantidade, itens);
            return new Expedicao(requisicao, guia, rota);
        }).map(consumo -> new Atendimento(consumo.resultado(), consumo.restantes()));
    }

    public record Expedicao(Requisicao requisicao, GuiaDeSaida guia, Roteirizador.Rota rota) {
    }

    public record Atendimento(Expedicao expedicao, int restantes) {
    }
}
