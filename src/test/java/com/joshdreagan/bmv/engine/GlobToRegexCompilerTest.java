package com.joshdreagan.bmv.engine;

import org.junit.jupiter.api.Test;

import java.util.regex.Matcher;

import static org.junit.jupiter.api.Assertions.*;

class GlobToRegexCompilerTest {

    @Test
    void testSimpleGlob() {
        var compiled = GlobToRegexCompiler.compile("(*).txt");
        Matcher m = compiled.regexPattern().matcher("foo.txt");
        assertTrue(m.matches());
        assertEquals("foo", m.group(1));

        assertFalse(compiled.regexPattern().matcher("dir/foo.txt").matches());
    }

    @Test
    void testRecursiveGlob() {
        var compiled = GlobToRegexCompiler.compile("(**/)(*).txt");
        assertTrue(compiled.isRecursive());

        Matcher m1 = compiled.regexPattern().matcher("foo.txt");
        assertTrue(m1.matches());
        assertEquals("", m1.group(1));
        assertEquals("foo", m1.group(2));

        Matcher m2 = compiled.regexPattern().matcher("sub/dir/foo.txt");
        assertTrue(m2.matches());
        assertEquals("sub/dir/", m2.group(1));
        assertEquals("foo", m2.group(2));
    }

    @Test
    void testBracketClasses() {
        var compiled = GlobToRegexCompiler.compile("([[:upper:]])(*).txt");
        Matcher m = compiled.regexPattern().matcher("Alpha.txt");
        assertTrue(m.matches());
        assertEquals("A", m.group(1));
        assertEquals("lpha", m.group(2));

        assertFalse(compiled.regexPattern().matcher("alpha.txt").matches());
    }
}
