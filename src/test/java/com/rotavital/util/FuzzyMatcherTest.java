package com.rotavital.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FuzzyMatcherTest {

    @Test
    void identicalStringsHaveZeroDistance() {
        assertEquals(0, FuzzyMatcher.calculateLevenshteinDistance("Dipirona", "Dipirona"));
    }

    @Test
    void oneCharacterInsertionHasDistanceOne() {
        assertEquals(1, FuzzyMatcher.calculateLevenshteinDistance("Dipirona", "Dipirrona"));
    }

    @Test
    void oneCharacterDeletionHasDistanceOne() {
        assertEquals(1, FuzzyMatcher.calculateLevenshteinDistance("Dipirona", "Dipirna"));
    }

    @Test
    void completelyDifferentWordsHaveHighDistance() {
        assertEquals(7, FuzzyMatcher.calculateLevenshteinDistance("Dipirona", "Xantina1"));
    }

    @Test
    void emptyStringDistanceEqualsOtherLength() {
        assertEquals(5, FuzzyMatcher.calculateLevenshteinDistance("", "abcde"));
    }
}
