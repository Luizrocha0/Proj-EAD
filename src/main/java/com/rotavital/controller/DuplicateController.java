package com.rotavital.controller;

import com.rotavital.service.DataGeneratorService;
import com.rotavital.service.DuplicateDetectionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/duplicates")
public class DuplicateController {

    private static final int MAX_SIZE = 20_000;
    private static final int MAX_THREADS = Runtime.getRuntime().availableProcessors() * 4;

    private final DataGeneratorService dataGeneratorService;
    private final DuplicateDetectionService duplicateDetectionService;

    public DuplicateController(DataGeneratorService dataGeneratorService, DuplicateDetectionService duplicateDetectionService) {
        this.dataGeneratorService = dataGeneratorService;
        this.duplicateDetectionService = duplicateDetectionService;
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

        if (size < 1 || size > MAX_SIZE) {
            response.put("status", "ERROR");
            response.put("message", "size deve estar entre 1 e " + MAX_SIZE);
            return response;
        }
        if (threads < 1 || threads > MAX_THREADS) {
            response.put("status", "ERROR");
            response.put("message", "threads deve estar entre 1 e " + MAX_THREADS);
            return response;
        }
        if (!mode.equals("platform") && !mode.equals("virtual")) {
            response.put("status", "ERROR");
            response.put("message", "mode deve ser 'platform' ou 'virtual'");
            return response;
        }

        try {
            long startTimeData = System.currentTimeMillis();
            List<String> records = dataGeneratorService.generateData(size);
            long dataGenerationTime = System.currentTimeMillis() - startTimeData;

            long startTimeProcess = System.currentTimeMillis();
            long duplicatesFound;

            if (mode.equals("virtual")) {
                duplicatesFound = duplicateDetectionService.detectParallelVirtual(records, threads);
            } else if (threads <= 1) {
                duplicatesFound = duplicateDetectionService.detectSequential(records);
            } else {
                duplicatesFound = duplicateDetectionService.detectParallel(records, threads);
            }

            long processingTime = System.currentTimeMillis() - startTimeProcess;

            response.put("dataGenerationTimeMs", dataGenerationTime);
            response.put("processingTimeMs", processingTime);
            response.put("duplicatesFound", duplicatesFound);
            response.put("status", "SUCCESS");

        } catch (Exception e) {
            response.put("status", "ERROR");
            response.put("message", "Erro ao processar a requisicao.");
        }

        return response;
    }
}
