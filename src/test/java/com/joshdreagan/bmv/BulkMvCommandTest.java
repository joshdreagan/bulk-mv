package com.joshdreagan.bmv;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class BulkMvCommandTest {

    @Test
    void testUsageWhenNoArgs() {
        BulkMvCommand cmd = new BulkMvCommand();
        CommandLine cl = new CommandLine(cmd);
        StringWriter errWriter = new StringWriter();
        cl.setErr(new PrintWriter(errWriter));

        int exitCode = cl.execute();
        assertEquals(1, exitCode);
    }

    @Test
    void testHelpOption() {
        BulkMvCommand cmd = new BulkMvCommand();
        CommandLine cl = new CommandLine(cmd);
        StringWriter outWriter = new StringWriter();
        cl.setOut(new PrintWriter(outWriter));

        int exitCode = cl.execute("--help");
        assertEquals(0, exitCode);
        assertTrue(outWriter.toString().contains("Move, copy or link files"));
        assertTrue(outWriter.toString().contains("--force"));
    }

    @Test
    void testVersionOption() {
        BulkMvCommand cmd = new BulkMvCommand();
        CommandLine cl = new CommandLine(cmd);
        StringWriter outWriter = new StringWriter();
        cl.setOut(new PrintWriter(outWriter));

        int exitCode = cl.execute("--version");
        assertEquals(0, exitCode);
        assertTrue(outWriter.toString().contains("bulk-mv 1.0.0"));
    }
}
