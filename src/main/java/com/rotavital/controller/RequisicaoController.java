package com.rotavital.controller;

import com.rotavital.domain.ConflitoAtendimentoException;
import com.rotavital.domain.Requisicao;
import com.rotavital.dto.AtendimentoRequestDTO;
import com.rotavital.dto.AtendimentoRequestDTO.PontoDTO;
import com.rotavital.dto.RequisicaoRequestDTO;
import com.rotavital.service.RequisicaoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

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
    public ResponseEntity<Map<String, Object>> enfileirar(@RequestBody RequisicaoRequestDTO body) {
        int tamanho = requisicaoService.enfileirar(body.toDomain());
        return ResponseEntity.ok(Map.of(
                "mensagem", "Requisição enfileirada",
                "tamanho", tamanho
        ));
    }

    /**
     * POST /api/requisicoes/atender — Confirma a baixa e a rota da próxima requisição.
     */
    @PostMapping("/atender")
    public ResponseEntity<Map<String, Object>> atender(@RequestBody AtendimentoRequestDTO body) {
        var resultado = requisicaoService.atenderProxima(body.requisicaoIdValidado(),
                PontoDTO.converter(body.origem()), body.paradasValidas(), PontoDTO.converter(body.destino()));
        return ResponseEntity.ok(resultado.<Map<String, Object>>map(atendimento -> Map.of(
                        "mensagem", "Requisição atendida",
                        "requisicao", resumir(atendimento.requisicao()),
                        "bolsas", atendimento.bolsas(),
                        "rota", atendimento.rota(),
                        "restantes", atendimento.restantes()
                ))
                .orElseGet(() -> Map.of("mensagem", "Fila vazia — nenhuma requisição pendente")));
    }

    /**
     * GET /api/requisicoes/proxima — Consulta a próxima requisição sem remover.
     */
    @GetMapping("/proxima")
    public ResponseEntity<Map<String, Object>> proxima() {
        return ResponseEntity.ok(requisicaoService.espiarProxima().map(this::resumir)
                .orElseGet(() -> Map.of("mensagem", "Fila vazia")));
    }

    /**
     * GET /api/requisicoes — Lista todas as requisições na fila.
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> listar() {
        var lista = Arrays.stream(requisicaoService.listarTodas()).map(this::resumir).toList();
        return ResponseEntity.ok(Map.of(
                "requisicoes", lista,
                "tamanho", lista.size()
        ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> requisicaoInvalida(IllegalArgumentException erro) {
        String mensagem = Objects.requireNonNullElse(erro.getMessage(), "Requisição inválida");
        return ResponseEntity.badRequest().body(Map.of("mensagem", mensagem));
    }

    @ExceptionHandler(ConflitoAtendimentoException.class)
    public ResponseEntity<Map<String, String>> conflitoDeAtendimento(ConflitoAtendimentoException erro) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("mensagem", erro.getMessage()));
    }

    private Map<String, Object> resumir(Requisicao requisicao) {
        return Map.of(
                "id", requisicao.getId(),
                "hospitalId", requisicao.getHospitalId(),
                "tipoSanguineo", requisicao.getTipoSanguineo(),
                "quantidadeBolsas", requisicao.getQuantidadeBolsas(),
                "prioridade", requisicao.getPrioridade().name()
        );
    }
}
