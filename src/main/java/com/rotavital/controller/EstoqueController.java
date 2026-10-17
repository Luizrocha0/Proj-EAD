package com.rotavital.controller;

import com.rotavital.domain.Bolsa;
import com.rotavital.dto.BolsaRequestDTO;
import com.rotavital.service.EstoqueService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Controller REST para operações no estoque de bolsas de sangue.
 * Apenas delega para {@link EstoqueService} — sem lógica de negócio aqui.
 */
@RestController
@RequestMapping("/api/estoque")
public class EstoqueController {

    private final EstoqueService estoqueService;

    public EstoqueController(EstoqueService estoqueService) {
        this.estoqueService = estoqueService;
    }

    /**
     * POST /api/estoque — Adiciona uma bolsa ao estoque.
     *
     * Body JSON: { "id", "tipoSanguineo", "volumeMl", "dataColeta", "validade", "doador" }
     */
    @PostMapping
    public ResponseEntity<?> adicionar(@RequestBody BolsaRequestDTO body) {
        if (body == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "mensagem", "Dados inválidos: corpo da requisição não pode ser nulo"
            ));
        }
        try {
            Bolsa bolsa = body.toDomain();
            estoqueService.adicionar(bolsa);
            return ResponseEntity.ok(Map.of(
                    "mensagem", "Bolsa adicionada ao estoque",
                    "tamanho", estoqueService.tamanho()
            ));
        } catch (IllegalArgumentException | java.time.format.DateTimeParseException | NullPointerException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "mensagem", "Dados inválidos: " + e.getMessage()
            ));
        }
    }

    /**
     * GET /api/estoque/{id} — Consulta uma bolsa pelo ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> consultar(@PathVariable String id) {
        if (id == null || id.isBlank()) {
            return ResponseEntity.notFound().build();
        }
        return estoqueService.consultar(id.trim())
                .map(bolsa -> ResponseEntity.ok((Object) Map.of(
                        "id", bolsa.getId(),
                        "tipoSanguineo", bolsa.getTipoSanguineo(),
                        "volumeMl", bolsa.getVolumeMl(),
                        "dataColeta", bolsa.getDataColeta().toString(),
                        "validade", bolsa.getValidade().toString(),
                        "doador", bolsa.getDoador()
                )))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * DELETE /api/estoque/{id} — Remove uma bolsa do estoque.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> remover(@PathVariable String id) {
        if (id == null || id.isBlank()) {
            return ResponseEntity.notFound().build();
        }
        boolean removido = estoqueService.remover(id.trim());
        if (removido) {
            return ResponseEntity.ok(Map.of(
                    "mensagem", "Bolsa removida",
                    "tamanho", estoqueService.tamanho()
            ));
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * GET /api/estoque — Lista todas as bolsas no estoque.
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> listar() {
        List<Bolsa> bolsas = estoqueService.listarEstoque();
        var lista = new java.util.ArrayList<Map<String, Object>>();
        for (Bolsa b : bolsas) {
            lista.add(Map.of(
                    "id", b.getId(),
                    "tipoSanguineo", b.getTipoSanguineo(),
                    "volumeMl", b.getVolumeMl(),
                    "dataColeta", b.getDataColeta().toString(),
                    "validade", b.getValidade().toString(),
                    "doador", b.getDoador()
            ));
        }
        return ResponseEntity.ok(Map.of(
                "bolsas", lista,
                "tamanho", estoqueService.tamanho()
        ));
    }
}
