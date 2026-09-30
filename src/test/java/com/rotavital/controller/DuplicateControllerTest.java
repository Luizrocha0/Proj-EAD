package com.rotavital.controller;

import com.rotavital.service.DataGeneratorService;
import com.rotavital.service.DuplicateDetectionService;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DuplicateControllerTest {

    private final DuplicateController controller =
            new DuplicateController(new DataGeneratorService(), new DuplicateDetectionService());

    @Test
    void rejectsSizeAboveUpperLimit() {
        Map<String, Object> response = controller.detectDuplicates(1_000_000, 1, "platform");
        assertEquals("ERROR", response.get("status"));
    }

    @Test
    void rejectsThreadsAboveUpperLimit() {
        Map<String, Object> response = controller.detectDuplicates(100, 100_000, "platform");
        assertEquals("ERROR", response.get("status"));
    }

    @Test
    void rejectsSizeBelowOne() {
        Map<String, Object> response = controller.detectDuplicates(0, 1, "platform");
        assertEquals("ERROR", response.get("status"));
    }

    @Test
    void rejectsUnknownMode() {
        Map<String, Object> response = controller.detectDuplicates(100, 1, "bogus");
        assertEquals("ERROR", response.get("status"));
    }

    @Test
    void acceptsValidRequestWithinLimits() {
        Map<String, Object> response = controller.detectDuplicates(100, 4, "platform");
        assertEquals("SUCCESS", response.get("status"));
    }
}
