package com.joshdreagan.bmv.engine;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReplacementProcessorTest {

    @Test
    void testPositionalReplacements() {
        String res = ReplacementProcessor.evaluate("$1.txt", "foo.lis", List.of("foo"));
        assertEquals("foo.txt", res);

        res = ReplacementProcessor.evaluate("${1}_${2}.out", "a_b", List.of("a", "b"));
        assertEquals("a_b.out", res);
    }

    @Test
    void testFileVariable() {
        String res = ReplacementProcessor.evaluate("backup_$f", "my_doc.txt", List.of());
        assertEquals("backup_my_doc.txt", res);

        res = ReplacementProcessor.evaluate("${f}", "data/file.csv", List.of());
        assertEquals("data/file.csv", res);
    }

    @Test
    void testCaseConversion() {
        String res = ReplacementProcessor.evaluate("${(L)1}.txt", "FILE.txt", List.of("FILE"));
        assertEquals("file.txt", res);

        res = ReplacementProcessor.evaluate("${(U)1}.txt", "file.txt", List.of("file"));
        assertEquals("FILE.txt", res);

        res = ReplacementProcessor.evaluate("${(C)1}.txt", "hello_world.txt", List.of("hello_world"));
        assertEquals("Hello_World.txt", res);
    }

    @Test
    void testSubstringReplacement() {
        String res = ReplacementProcessor.evaluate("${1// /_}.txt", "foo bar baz.txt", List.of("foo bar baz"));
        assertEquals("foo_bar_baz.txt", res);

        res = ReplacementProcessor.evaluate("${f// /_}", "foo bar baz.txt", List.of());
        assertEquals("foo_bar_baz.txt", res);

        res = ReplacementProcessor.evaluate("${1/foo/bar}.txt", "foofoo.txt", List.of("foofoo"));
        assertEquals("barfoo.txt", res);
    }

    @Test
    void testModifiers() {
        assertEquals("file", ReplacementProcessor.evaluate("${f:t:r}", "dir/subdir/file.ext", List.of()));
        assertEquals("ext", ReplacementProcessor.evaluate("${f:e}", "dir/subdir/file.ext", List.of()));
        assertEquals("dir/subdir", ReplacementProcessor.evaluate("${f:h}", "dir/subdir/file.ext", List.of()));
        assertEquals("file.ext", ReplacementProcessor.evaluate("${f:t}", "dir/subdir/file.ext", List.of()));
        assertEquals("dir/subdir/file", ReplacementProcessor.evaluate("${f:r}", "dir/subdir/file.ext", List.of()));
    }
}
