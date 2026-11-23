package com.rotavital.requisicoes.apresentacao;

import com.rotavital.requisicoes.negocio.aplicacao.RequisicaoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/requisicoes")
public class RequisicaoController {
    private final RequisicaoService requisicoes;

    public RequisicaoController(RequisicaoService requisicoes) {
        this.requisicoes = requisicoes;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> enfileirar(@RequestBody RequisicaoRequestDTO body) {
        int tamanho = requisicoes.enfileirar(body.toDomain());
        return ResponseEntity.ok(Map.of("mensagem", "Requisição enfileirada", "tamanho", tamanho));
    }

    @GetMapping("/proxima")
    public ResponseEntity<Object> proxima() {
        return ResponseEntity.ok(requisicoes.espiarProxima().<Object>map(RequisicaoResponse::de)
                .orElseGet(() -> Map.of("mensagem", "Fila vazia")));
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> listar() {
        var lista = requisicoes.listarTodas().stream().map(RequisicaoResponse::de).toList();
        return ResponseEntity.ok(Map.of("requisicoes", lista, "tamanho", lista.size()));
    }
}
