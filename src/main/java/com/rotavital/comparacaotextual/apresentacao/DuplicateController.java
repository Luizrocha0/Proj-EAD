package com.rotavital.comparacaotextual.apresentacao;

import com.rotavital.comparacaotextual.negocio.aplicacao.DetectarPares;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api/duplicates")
public class DuplicateController {
    private final DetectarPares detectarPares;

    public DuplicateController(DetectarPares detectarPares) {
        this.detectarPares = detectarPares;
    }

    @GetMapping("/detect")
    public Map<String, Object> detectDuplicates(
            @RequestParam(defaultValue = "10000") int size,
            @RequestParam(defaultValue = "1") int threads,
            @RequestParam(defaultValue = "platform") String mode) {
        Map<String, Object> response = new HashMap<>();
        response.put("datasetSize", size);
        response.put("threads", threads);
        response.put("mode", mode);

        try {
            var medicao = detectarPares.executar(size, threads, mode);
            response.put("dataGenerationTimeMs", medicao.dataGenerationTimeMs());
            response.put("processingTimeMs", medicao.processingTimeMs());
            response.put("duplicatesFound", medicao.duplicatesFound());
            response.put("status", "SUCCESS");
        } catch (IllegalArgumentException e) {
            erro(response, e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            erro(response, "Erro ao processar a requisicao.");
        } catch (ExecutionException e) {
            erro(response, "Erro ao processar a requisicao.");
        }

        return response;
    }

    private void erro(Map<String, Object> response, String mensagem) {
        response.put("status", "ERROR");
        response.put("message", mensagem);
    }
}
