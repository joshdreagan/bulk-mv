package com.joshdreagan.bmv.engine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PatternTransformerTest {

    @Test
    void testNoWildcardFlag() {
        var res = PatternTransformer.transform("(*).txt", "$1.lis", false, false);
        assertEquals("(*).txt", res.searchPattern());
        assertEquals("$1.lis", res.replacementPattern());
        assertEquals(0, res.searchWildcardCount());
    }

    @Test
    void testOptW() {
        var res = PatternTransformer.transform("*.txt", "$1.lis", true, false);
        assertEquals("(*).txt", res.searchPattern());
        assertEquals("$1.lis", res.replacementPattern());
        assertEquals(1, res.searchWildcardCount());
    }

    @Test
    void testOptWRecursive() {
        var res = PatternTransformer.transform("**/*.txt", "$1$2.lis", true, false);
        assertEquals("(**/)(*).txt", res.searchPattern());
        assertEquals("$1$2.lis", res.replacementPattern());
        assertEquals(2, res.searchWildcardCount());
    }

    @Test
    void testOptBigW() {
        var res = PatternTransformer.transform("*.lis", "*.txt", false, true);
        assertEquals("(*).lis", res.searchPattern());
        assertEquals("${1}.txt", res.replacementPattern());
        assertEquals(1, res.searchWildcardCount());
        assertEquals(1, res.replacementWildcardCount());
    }

    @Test
    void testOptBigWMultiple() {
        var res = PatternTransformer.transform("**/*.lis", "**/*.txt", false, true);
        assertEquals("(**/)(*).lis", res.searchPattern());
        assertEquals("${1}${2}.txt", res.replacementPattern());
        assertEquals(2, res.searchWildcardCount());
        assertEquals(2, res.replacementWildcardCount());
    }

    @Test
    void testCharacterClasses() {
        var res = PatternTransformer.transform("[[:upper:]]*", "${(L)1}$2", true, false);
        assertEquals("([[:upper:]])(*)", res.searchPattern());
        assertEquals(2, res.searchWildcardCount());
    }
}
