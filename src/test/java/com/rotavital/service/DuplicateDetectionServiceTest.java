package com.rotavital.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DuplicateDetectionServiceTest {

    private final DuplicateDetectionService service = new DuplicateDetectionService();

    private List<String> buildDataset(int size) {
        String[] base = {"Dipirona", "Paracetamol", "Ibuprofeno", "Amoxicilina", "Azitromicina"};
        Random random = new Random(7);
        List<String> data = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            String word = base[random.nextInt(base.length)];
            if (random.nextDouble() < 0.2 && word.length() > 2) {
                StringBuilder sb = new StringBuilder(word);
                sb.setCharAt(1, (char) (sb.charAt(1) + 1));
                word = sb.toString();
            }
            data.add(word);
        }
        return data;
    }

    @Test
    void sequentialCountsKnownDuplicatesInTinySet() {
        List<String> records = List.of("Dipirona", "Dipirona", "Xantina");
        assertEquals(1, service.detectSequential(records));
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 4, 8})
    void parallelMatchesSequentialForVariousThreadCounts(int threads) throws Exception {
        List<String> records = buildDataset(500);
        long expected = service.detectSequential(records);
        long actual = service.detectParallel(records, threads);
        assertEquals(expected, actual, "resultado paralelo (" + threads + " threads) divergiu do sequencial - possivel race condition");
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 4, 8})
    void virtualThreadsMatchSequential(int tasks) throws Exception {
        List<String> records = buildDataset(500);
        long expected = service.detectSequential(records);
        long actual = service.detectParallelVirtual(records, tasks);
        assertEquals(expected, actual, "resultado com virtual threads divergiu do sequencial");
    }

    @Test
    void parallelWithMoreThreadsThanRecordsStillWorks() throws Exception {
        List<String> records = buildDataset(3);
        long expected = service.detectSequential(records);
        assertEquals(expected, service.detectParallel(records, 8));
    }

    @Test
    void emptyDatasetReturnsZeroForAllVersions() throws Exception {
        List<String> empty = List.of();
        assertEquals(0, service.detectSequential(empty));
        assertEquals(0, service.detectParallel(empty, 4));
        assertEquals(0, service.detectParallelVirtual(empty, 4));
    }

    @Test
    void detectParallelRejectsInvalidThreadCount() {
        List<String> records = buildDataset(10);
        assertThrows(IllegalArgumentException.class, () -> service.detectParallel(records, 0));
    }

    @Test
    void detectParallelVirtualRejectsInvalidTaskCount() {
        List<String> records = buildDataset(10);
        assertThrows(IllegalArgumentException.class, () -> service.detectParallelVirtual(records, -1));
    }
}
