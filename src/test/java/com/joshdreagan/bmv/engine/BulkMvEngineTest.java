package com.joshdreagan.bmv.engine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class BulkMvEngineTest {

    @TempDir
    Path tempDir;

    private ByteArrayOutputStream outContent;
    private ByteArrayOutputStream errContent;

    @BeforeEach
    void setUp() {
        outContent = new ByteArrayOutputStream();
        errContent = new ByteArrayOutputStream();
    }

    private BulkMvEngine.Options createOptions() {
        BulkMvEngine.Options options = new BulkMvEngine.Options();
        options.workingDir = tempDir;
        options.out = new PrintStream(outContent);
        options.err = new PrintStream(errContent);
        return options;
    }

    @Test
    void testSimpleMove() throws IOException {
        Files.writeString(tempDir.resolve("file1.txt"), "hello");
        Files.writeString(tempDir.resolve("file2.txt"), "world");

        var options = createOptions();
        int status = BulkMvEngine.run(options, "(*).txt", "$1.lis");

        assertEquals(0, status);
        assertFalse(Files.exists(tempDir.resolve("file1.txt")));
        assertFalse(Files.exists(tempDir.resolve("file2.txt")));
        assertTrue(Files.exists(tempDir.resolve("file1.lis")));
        assertTrue(Files.exists(tempDir.resolve("file2.lis")));
        assertEquals("hello", Files.readString(tempDir.resolve("file1.lis")));
        assertEquals("world", Files.readString(tempDir.resolve("file2.lis")));
    }

    @Test
    void testMoveWithOptBigW() throws IOException {
        Files.writeString(tempDir.resolve("doc1.c.orig"), "content1");
        Files.writeString(tempDir.resolve("doc2.c.orig"), "content2");

        var options = createOptions();
        options.withWildcardsAndReplacement = true;
        int status = BulkMvEngine.run(options, "*.c.orig", "orig/*.c");

        assertEquals(0, status);
        assertTrue(Files.exists(tempDir.resolve("orig/doc1.c")));
        assertTrue(Files.exists(tempDir.resolve("orig/doc2.c")));
        assertFalse(Files.exists(tempDir.resolve("doc1.c.orig")));
        assertFalse(Files.exists(tempDir.resolve("doc2.c.orig")));
    }

    @Test
    void testDryRun() throws IOException {
        Files.writeString(tempDir.resolve("sample.txt"), "text");

        var options = createOptions();
        options.dryRun = true;
        int status = BulkMvEngine.run(options, "(*).txt", "$1.bak");

        assertEquals(0, status);
        assertTrue(Files.exists(tempDir.resolve("sample.txt")));
        assertFalse(Files.exists(tempDir.resolve("sample.bak")));
        assertTrue(outContent.toString().contains("mv -- sample.txt sample.bak"));
    }

    @Test
    void testCopyAction() throws IOException {
        Files.writeString(tempDir.resolve("source.txt"), "data");

        var options = createOptions();
        options.action = ActionType.CP;
        int status = BulkMvEngine.run(options, "(*).txt", "$1.copy");

        assertEquals(0, status);
        assertTrue(Files.exists(tempDir.resolve("source.txt")));
        assertTrue(Files.exists(tempDir.resolve("source.copy")));
    }

    @Test
    void testCollisionError() throws IOException {
        Files.writeString(tempDir.resolve("a.txt"), "a");
        Files.writeString(tempDir.resolve("b.txt"), "b");

        var options = createOptions();
        int status = BulkMvEngine.run(options, "*.txt", "c.txt");

        assertEquals(1, status);
        assertTrue(errContent.toString().contains("both map to c.txt"));
        // No files should have been renamed/deleted
        assertTrue(Files.exists(tempDir.resolve("a.txt")));
        assertTrue(Files.exists(tempDir.resolve("b.txt")));
    }

    @Test
    void testExistingFileErrorWithoutForce() throws IOException {
        Files.writeString(tempDir.resolve("orig.txt"), "1");
        Files.writeString(tempDir.resolve("dest.txt"), "2");

        var options = createOptions();
        int status = BulkMvEngine.run(options, "(orig).txt", "dest.txt");

        assertEquals(1, status);
        assertTrue(errContent.toString().contains("file exists: dest.txt"));
        assertEquals("1", Files.readString(tempDir.resolve("orig.txt")));
        assertEquals("2", Files.readString(tempDir.resolve("dest.txt")));
    }

    @Test
    void testExistingFileOverwriteWithForce() throws IOException {
        Files.writeString(tempDir.resolve("orig.txt"), "new content");
        Files.writeString(tempDir.resolve("dest.txt"), "old content");

        var options = createOptions();
        options.force = true;
        int status = BulkMvEngine.run(options, "(orig).txt", "dest.txt");

        assertEquals(0, status);
        assertFalse(Files.exists(tempDir.resolve("orig.txt")));
        assertTrue(Files.exists(tempDir.resolve("dest.txt")));
        assertEquals("new content", Files.readString(tempDir.resolve("dest.txt")));
    }

    @Test
    void testInteractiveExecution() throws IOException {
        Files.writeString(tempDir.resolve("test1.txt"), "1");
        Files.writeString(tempDir.resolve("test2.txt"), "2");

        var options = createOptions();
        options.interactive = true;
        // User answers 'y' for test1, 'n' for test2
        options.in = new ByteArrayInputStream("y\nn\n".getBytes());

        int status = BulkMvEngine.run(options, "(*).txt", "$1.done");

        assertEquals(0, status);
        assertTrue(Files.exists(tempDir.resolve("test1.done")));
        assertFalse(Files.exists(tempDir.resolve("test1.txt")));

        assertTrue(Files.exists(tempDir.resolve("test2.txt")));
        assertFalse(Files.exists(tempDir.resolve("test2.done")));
    }

    @Test
    void testInvalidOptionSymbolicWithoutLink() {
        var options = createOptions();
        options.action = ActionType.MV;
        options.symbolic = true;

        int status = BulkMvEngine.run(options, "(*).txt", "$1.out");
        assertEquals(1, status);
        assertTrue(errContent.toString().contains("invalid option: -s"));
    }
}
