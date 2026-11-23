package com.rotavital.comparacaotextual.negocio.aplicacao;

import com.rotavital.comparacaotextual.negocio.dominio.DuplicateDetectionService;

import java.util.concurrent.ExecutionException;

public class DetectarPares {
    private static final int MAX_SIZE = 20_000;
    private static final int MAX_THREADS = Runtime.getRuntime().availableProcessors() * 4;

    private final GeradorDeAmostras gerador;
    private final DuplicateDetectionService detector;

    public DetectarPares(GeradorDeAmostras gerador, DuplicateDetectionService detector) {
        this.gerador = gerador;
        this.detector = detector;
    }

    public Medicao executar(int size, int threads, String mode) throws InterruptedException, ExecutionException {
        validar(size, threads, mode);
        long inicioGeracao = System.currentTimeMillis();
        var amostras = gerador.generateData(size);
        long inicioProcessamento = System.currentTimeMillis();

        long pares;
        if (mode.equals("virtual")) {
            pares = detector.detectParallelVirtual(amostras, threads);
        } else if (threads == 1) {
            pares = detector.detectSequential(amostras);
        } else {
            pares = detector.detectParallel(amostras, threads);
        }

        return new Medicao(inicioProcessamento - inicioGeracao,
                System.currentTimeMillis() - inicioProcessamento, pares);
    }

    private void validar(int size, int threads, String mode) {
        if (size < 1 || size > MAX_SIZE) {
            throw new IllegalArgumentException("size deve estar entre 1 e " + MAX_SIZE);
        }
        if (threads < 1 || threads > MAX_THREADS) {
            throw new IllegalArgumentException("threads deve estar entre 1 e " + MAX_THREADS);
        }
        if (!"platform".equals(mode) && !"virtual".equals(mode)) {
            throw new IllegalArgumentException("mode deve ser 'platform' ou 'virtual'");
        }
    }

    public record Medicao(long dataGenerationTimeMs, long processingTimeMs, long duplicatesFound) {
    }
}
