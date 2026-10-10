package com.joshdreagan.bmv.engine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BulkMvEngine {

    public static class Options {
        public ActionType action = ActionType.MV;
        public boolean force = false;
        public boolean interactive = false;
        public boolean dryRun = false;
        public boolean verbose = false;
        public boolean symbolic = false;
        public boolean withWildcards = false;
        public boolean withWildcardsAndReplacement = false;
        public boolean bareGlobQualifiers = false;
        public String program = null;
        public boolean programNoDashes = false;
        public String optString = null;
        public String commandName = "bulk-mv";
        public Path workingDir = Path.of(".");
        public InputStream in = System.in;
        public PrintStream out = System.out;
        public PrintStream err = System.err;
    }

    public static int run(Options options, String searchPattern, String replacementPattern) {
        // Validate options
        if (options.symbolic && options.action != ActionType.LN) {
            options.err.println(options.commandName + ": invalid option: -s");
            return 1;
        }

        // Apply -w / -W transformations
        PatternTransformer.TransformResult transformed = PatternTransformer.transform(
                searchPattern,
                replacementPattern,
                options.withWildcards,
                options.withWildcardsAndReplacement
        );

        if (options.withWildcards || options.withWildcardsAndReplacement) {
            if (transformed.searchWildcardCount() == 0) {
                options.err.println(options.commandName + ": warning: no wildcards were found in search pattern");
            }
            if (options.withWildcardsAndReplacement) {
                if (transformed.replacementWildcardCount() == 0) {
                    options.err.println(options.commandName + ": warning: no wildcards were found in replacement pattern");
                } else if (transformed.searchWildcardCount() != transformed.replacementWildcardCount()) {
                    options.err.println(options.commandName + ": error: number of wildcards in each pattern must match");
                    return 1;
                }
            }
        }

        // Compile glob to regex
        GlobToRegexCompiler.CompiledPattern compiled = GlobToRegexCompiler.compile(transformed.searchPattern());

        // Scan files
        List<FileScanner.MatchedFile> matchedFiles;
        try {
            matchedFiles = FileScanner.scan(compiled, options.workingDir);
        } catch (IOException e) {
            options.err.println(options.commandName + ": error scanning files: " + e.getMessage());
            return 1;
        }

        if (matchedFiles.isEmpty()) {
            return 0;
        }

        // Plan substitutions and validations
        List<String> errors = new ArrayList<>();
        Map<String, String> fromMap = new LinkedHashMap<>(); // target -> source
        Map<String, String> toMap = new LinkedHashMap<>();   // source -> target

        for (FileScanner.MatchedFile mf : matchedFiles) {
            String f = mf.relativePath();
            String g = ReplacementProcessor.evaluate(transformed.replacementPattern(), f, mf.captureGroups());

            if (g == null || g.isEmpty()) {
                errors.add("`" + f + "' expanded to an empty string");
                continue;
            }

            if (f.equals(g)) {
                if (options.verbose) {
                    options.out.println(f + " not altered, ignored");
                }
                continue;
            }

            if (fromMap.containsKey(g)) {
                Path targetPath = options.workingDir.resolve(g);
                if (!Files.isDirectory(targetPath)) {
                    errors.add(f + " and " + fromMap.get(g) + " both map to " + g);
                }
            }

            Path targetPath = options.workingDir.resolve(g);
            Path srcPath = options.workingDir.resolve(f);
            if (Files.exists(targetPath) && !options.force) {
                boolean sameFile = false;
                try {
                    sameFile = Files.isSameFile(srcPath, targetPath);
                } catch (IOException ignored) {
                }
                if (!sameFile || options.action != ActionType.MV) {
                    errors.add("file exists: " + g);
                }
            }

            fromMap.put(g, f);
            toMap.put(f, g);
        }

        if (!errors.isEmpty()) {
            options.err.println(options.commandName + ": error(s) in substitution:");
            for (String err : errors) {
                options.err.println(err);
            }
            return 1;
        }

        // Execute actions
        BufferedReader reader = new BufferedReader(new InputStreamReader(options.in));
        int status = 0;

        for (FileScanner.MatchedFile mf : matchedFiles) {
            String f = mf.relativePath();
            String g = toMap.get(f);
            if (g == null) {
                continue;
            }

            List<String> cmdArgs = buildCommandArgs(options, f, g);
            String cmdDisplay = String.join(" ", cmdArgs);

            if (options.dryRun || options.verbose || options.interactive) {
                options.out.println(cmdDisplay);
            }

            if (options.interactive) {
                options.out.print("Execute? ");
                options.out.flush();
                try {
                    String line = reader.readLine();
                    if (line == null || (!line.startsWith("y") && !line.startsWith("Y"))) {
                        continue;
                    }
                } catch (IOException e) {
                    options.err.println(options.commandName + ": error reading input: " + e.getMessage());
                    return 1;
                }
            }

            if (!options.dryRun) {
                boolean success = executeAction(options, f, g);
                if (!success) {
                    status = 1;
                }
            }
        }

        return status;
    }

    private static List<String> buildCommandArgs(Options options, String f, String g) {
        List<String> cmd = new ArrayList<>();
        String actionName;
        if (options.action == ActionType.CUSTOM) {
            actionName = options.program;
        } else {
            actionName = options.action.name().toLowerCase();
        }

        cmd.add(actionName);

        if (options.optString != null && !options.optString.isBlank()) {
            for (String part : options.optString.trim().split("\\s+")) {
                if (!part.isEmpty()) {
                    cmd.add(part);
                }
            }
        }

        if (options.action == ActionType.LN && options.symbolic) {
            cmd.add("-s");
        }

        if (options.action != ActionType.CUSTOM || !options.programNoDashes) {
            cmd.add("--");
        }

        cmd.add(f);
        cmd.add(g);
        return cmd;
    }

    private static boolean executeAction(Options options, String f, String g) {
        Path src = options.workingDir.resolve(f);
        Path dest = options.workingDir.resolve(g);

        try {
            Path destParent = dest.getParent();
            if (destParent != null && !Files.exists(destParent)) {
                Files.createDirectories(destParent);
            }

            if (options.action == ActionType.CUSTOM) {
                List<String> cmd = buildCommandArgs(options, f, g);
                ProcessBuilder pb = new ProcessBuilder(cmd);
                pb.directory(options.workingDir.toFile());
                pb.inheritIO();
                Process process = pb.start();
                int exitCode = process.waitFor();
                return exitCode == 0;
            }

            switch (options.action) {
                case MV -> {
                    if (options.force) {
                        Files.move(src, dest, StandardCopyOption.REPLACE_EXISTING);
                    } else {
                        Files.move(src, dest);
                    }
                }
                case CP -> {
                    if (options.force) {
                        Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
                    } else {
                        Files.copy(src, dest);
                    }
                }
                case LN -> {
                    if (options.force && Files.exists(dest)) {
                        Files.delete(dest);
                    }
                    if (options.symbolic) {
                        Files.createSymbolicLink(dest, src);
                    } else {
                        Files.createLink(dest, src);
                    }
                }
            }
            return true;
        } catch (Exception e) {
            options.err.println(options.commandName + ": " + e.getMessage());
            return false;
        }
    }
}
