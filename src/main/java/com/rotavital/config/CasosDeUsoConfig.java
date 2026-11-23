package com.rotavital.config;

import com.rotavital.comparacaotextual.negocio.aplicacao.DetectarPares;
import com.rotavital.comparacaotextual.negocio.aplicacao.GeradorDeAmostras;
import com.rotavital.comparacaotextual.negocio.dominio.DuplicateDetectionService;
import com.rotavital.estoque.negocio.aplicacao.EstoqueService;
import com.rotavital.estoque.negocio.aplicacao.EstoqueStorage;
import com.rotavital.expedicao.negocio.aplicacao.AtenderRequisicao;
import com.rotavital.expedicao.negocio.aplicacao.BaixaParaExpedicao;
import com.rotavital.requisicoes.negocio.aplicacao.FilaDeRequisicoes;
import com.rotavital.requisicoes.negocio.aplicacao.RequisicaoService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration(proxyBeanMethods = false)
public class CasosDeUsoConfig {
    @Bean
    EstoqueService estoqueService(EstoqueStorage storage) {
        return new EstoqueService(storage);
    }

    @Bean
    RequisicaoService requisicaoService(FilaDeRequisicoes fila) {
        return new RequisicaoService(fila);
    }

    @Bean
    AtenderRequisicao atenderRequisicao(RequisicaoService requisicoes, BaixaParaExpedicao estoque) {
        return new AtenderRequisicao(requisicoes, estoque, Clock.systemDefaultZone());
    }

    @Bean
    DetectarPares detectarPares(GeradorDeAmostras gerador) {
        return new DetectarPares(gerador, new DuplicateDetectionService());
    }
}
