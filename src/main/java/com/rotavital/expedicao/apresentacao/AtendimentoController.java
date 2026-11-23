package com.rotavital.expedicao.apresentacao;

import com.rotavital.expedicao.apresentacao.AtendimentoRequestDTO.PontoDTO;
import com.rotavital.expedicao.negocio.aplicacao.AtenderRequisicao;
import com.rotavital.requisicoes.apresentacao.RequisicaoResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

@RestController
public class AtendimentoController {
    private final AtenderRequisicao atenderRequisicao;

    public AtendimentoController(AtenderRequisicao atenderRequisicao) {
        this.atenderRequisicao = atenderRequisicao;
    }

    @PostMapping("/api/requisicoes/atender")
    public ResponseEntity<Map<String, Object>> atender(@RequestBody AtendimentoRequestDTO body) {
        var resultado = atenderRequisicao.executar(body.requisicaoIdValidado(), PontoDTO.converter(body.origem()),
                body.paradasValidas(), PontoDTO.converter(body.destino()));
        return ResponseEntity.ok(resultado.map(this::resumir)
                .orElseGet(() -> Map.of("mensagem", "Fila vazia — nenhuma requisição pendente")));
    }

    private Map<String, Object> resumir(AtenderRequisicao.Atendimento atendimento) {
        var expedicao = atendimento.expedicao();
        var bolsas = expedicao.guia().itens().stream()
                .map(item -> new BolsaAtendida(item.id(), item.tipoSanguineo(), item.volumeMl(), item.validade()))
                .toList();
        return Map.of("mensagem", "Requisição atendida", "requisicao", RequisicaoResponse.de(expedicao.requisicao()),
                "bolsas", bolsas, "rota", expedicao.rota(), "restantes", atendimento.restantes());
    }

    private record BolsaAtendida(String id, String tipoSanguineo, double volumeMl, LocalDate validade) {
    }
}
