package com.rotavital.controller;

import com.rotavital.domain.Requisicao;
import com.rotavital.service.RequisicaoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Controller REST para operações na fila de requisições emergenciais.
 * Apenas delega para {@link RequisicaoService} — sem lógica de negócio aqui.
 */
@RestController
@RequestMapping("/api/requisicoes")
public class RequisicaoController {

    private final RequisicaoService requisicaoService;

    public RequisicaoController(RequisicaoService requisicaoService) {
        this.requisicaoService = requisicaoService;
    }

    /**
     * POST /api/requisicoes — Enfileira uma nova requisição.
     *
     * Body JSON: { "id", "hospitalId", "tipoSanguineo", "quantidadeBolsas", "prioridade" }
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> enfileirar(@RequestBody Map<String, String> body) {
        Requisicao req = new Requisicao(
                body.get("id"),
                body.get("hospitalId"),
                body.get("tipoSanguineo"),
                Integer.parseInt(body.getOrDefault("quantidadeBolsas", "1")),
                Requisicao.Prioridade.valueOf(body.getOrDefault("prioridade", "NORMAL")),
                LocalDateTime.now()
        );
        requisicaoService.enfileirar(req);
        return ResponseEntity.ok(Map.of(
                "mensagem", "Requisição enfileirada",
                "tamanho", requisicaoService.tamanho()
        ));
    }

    /**
     * POST /api/requisicoes/atender — Desenfileira e atende a próxima requisição (FIFO).
     */
    @PostMapping("/atender")
    public ResponseEntity<?> atender() {
        return requisicaoService.atenderProxima()
                .map(req -> ResponseEntity.ok((Object) Map.of(
                        "mensagem", "Requisição atendida",
                        "requisicao", Map.of(
                                "id", req.getId(),
                                "hospitalId", req.getHospitalId(),
                                "tipoSanguineo", req.getTipoSanguineo(),
                                "quantidadeBolsas", req.getQuantidadeBolsas(),
                                "prioridade", req.getPrioridade().name()
                        ),
                        "restantes", requisicaoService.tamanho()
                )))
                .orElse(ResponseEntity.ok(Map.of("mensagem", "Fila vazia — nenhuma requisição pendente")));
    }

    /**
     * GET /api/requisicoes/proxima — Consulta a próxima requisição sem remover.
     */
    @GetMapping("/proxima")
    public ResponseEntity<?> proxima() {
        return requisicaoService.espiarProxima()
                .map(req -> ResponseEntity.ok((Object) Map.of(
                        "id", req.getId(),
                        "hospitalId", req.getHospitalId(),
                        "tipoSanguineo", req.getTipoSanguineo(),
                        "quantidadeBolsas", req.getQuantidadeBolsas(),
                        "prioridade", req.getPrioridade().name()
                )))
                .orElse(ResponseEntity.ok(Map.of("mensagem", "Fila vazia")));
    }

    /**
     * GET /api/requisicoes — Lista todas as requisições na fila.
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> listar() {
        Requisicao[] reqs = requisicaoService.listarTodas();
        var lista = new java.util.ArrayList<Map<String, Object>>();
        for (Requisicao r : reqs) {
            lista.add(Map.of(
                    "id", r.getId(),
                    "hospitalId", r.getHospitalId(),
                    "tipoSanguineo", r.getTipoSanguineo(),
                    "quantidadeBolsas", r.getQuantidadeBolsas(),
                    "prioridade", r.getPrioridade().name()
            ));
        }
        return ResponseEntity.ok(Map.of(
                "requisicoes", lista,
                "tamanho", requisicaoService.tamanho()
        ));
    }
}
