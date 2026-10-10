package com.joshdreagan.bmv.engine;

import org.wildfly.common.annotation.NotNull;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileScanner {

    public record MatchedFile(String relativePath, Path fullPath, List<String> captureGroups) {
    }

    public static List<MatchedFile> scan(GlobToRegexCompiler.CompiledPattern compiledPattern, Path workingDir) throws IOException {
        Path baseSearch = workingDir.resolve(compiledPattern.searchBaseDir()).normalize();

        if (!Files.exists(baseSearch)) {
            return Collections.emptyList();
        }

        List<Path> discoveredPaths = new ArrayList<>();

        if (compiledPattern.isRecursive() || Files.isDirectory(baseSearch)) {
            int maxDepth = compiledPattern.isRecursive() ? Integer.MAX_VALUE : calculateDepth(compiledPattern.originalPattern());
            Files.walkFileTree(baseSearch, Collections.emptySet(), maxDepth, new SimpleFileVisitor<>() {
                @Override
                public @NotNull FileVisitResult visitFile(@NotNull Path file, @NotNull BasicFileAttributes attrs) {
                    discoveredPaths.add(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public @NotNull FileVisitResult preVisitDirectory(@NotNull Path dir, @NotNull BasicFileAttributes attrs) {
                    if (!dir.equals(baseSearch) && !dir.equals(workingDir)) {
                        discoveredPaths.add(dir);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } else {
            discoveredPaths.add(baseSearch);
        }

        // Sort depth-first (deeper paths / longer paths first)
        discoveredPaths.sort((p1, p2) -> {
            int depth1 = p1.getNameCount();
            int depth2 = p2.getNameCount();
            if (depth1 != depth2) {
                return Integer.compare(depth2, depth1); // descending depth
            }
            return p1.toString().compareTo(p2.toString());
        });

        List<MatchedFile> matchedFiles = new ArrayList<>();
        Pattern regex = compiledPattern.regexPattern();

        for (Path p : discoveredPaths) {
            Path rel = workingDir.relativize(p);
            String relStr = rel.toString().replace('\\', '/');

            Matcher m = regex.matcher(relStr);
            if (m.matches()) {
                List<String> groups = new ArrayList<>();
                for (int g = 1; g <= m.groupCount(); g++) {
                    groups.add(m.group(g));
                }
                matchedFiles.add(new MatchedFile(relStr, p, groups));
            }
        }

        return matchedFiles;
    }

    private static int calculateDepth(String glob) {
        int depth = 1;
        for (int i = 0; i < glob.length(); i++) {
            if (glob.charAt(i) == '/') {
                depth++;
            }
        }
        return Math.max(depth + 1, 10);
    }
}
