package com.rotavital.service;

import com.rotavital.util.FuzzyMatcher;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.ArrayList;

@Service
public class DuplicateDetectionService {

    private static final int MAX_DISTANCE_FOR_DUPLICATE = 2;


    public long detectSequential(List<String> records) {
        long duplicateCount = 0;
        int size = records.size();

        for (int i = 0; i < size; i++) {
            String target = records.get(i);
            for (int j = i + 1; j < size; j++) {
                String candidate = records.get(j);
                if (FuzzyMatcher.calculateLevenshteinDistance(target, candidate) <= MAX_DISTANCE_FOR_DUPLICATE) {
                    duplicateCount++;
                }
            }
        }
        return duplicateCount;
    }


    public long detectParallel(List<String> records, int numThreads) throws InterruptedException, ExecutionException {
        if (numThreads < 1) {
            throw new IllegalArgumentException("numThreads deve ser >= 1");
        }
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        try {
            return runPartitioned(records, numThreads, executor);
        } finally {
            executor.shutdown();
        }
    }

    public long detectParallelVirtual(List<String> records, int numTasks) throws InterruptedException, ExecutionException {
        if (numTasks < 1) {
            throw new IllegalArgumentException("numTasks deve ser >= 1");
        }
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            return runPartitioned(records, numTasks, executor);
        }
    }

    private long runPartitioned(List<String> records, int slices, ExecutorService executor) throws InterruptedException, ExecutionException {
        int size = records.size();
        List<Callable<Long>> tasks = new ArrayList<>();

        int chunkSize = (int) Math.ceil((double) size / slices);

        for (int t = 0; t < slices; t++) {
            final int startIdx = t * chunkSize;
            final int endIdx = Math.min(startIdx + chunkSize, size);

            tasks.add(() -> {
                long localDuplicates = 0;
                for (int i = startIdx; i < endIdx; i++) {
                    String target = records.get(i);
                    for (int j = i + 1; j < size; j++) {
                        String candidate = records.get(j);
                        if (FuzzyMatcher.calculateLevenshteinDistance(target, candidate) <= MAX_DISTANCE_FOR_DUPLICATE) {
                            localDuplicates++;
                        }
                    }
                }
                return localDuplicates;
            });
        }

        long totalDuplicates = 0;
        List<Future<Long>> results = executor.invokeAll(tasks);

        for (Future<Long> result : results) {
            totalDuplicates += result.get();
        }

        return totalDuplicates;
    }
}
