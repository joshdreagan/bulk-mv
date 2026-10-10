package com.joshdreagan.bmv.engine;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

public class GlobToRegexCompiler {

    public record CompiledPattern(
            Pattern regexPattern,
            Path searchBaseDir,
            boolean isRecursive,
            String originalPattern
    ) {
    }

    public static CompiledPattern compile(String globPattern) {
      // Extract base search directory (literal path before any wildcard or parenthesis)
        Path baseDir = extractBaseDir(globPattern);

        // Build regex
        StringBuilder regex = new StringBuilder("^");
        int len = globPattern.length();
        int i = 0;
        boolean recursive = globPattern.contains("**");

        while (i < len) {
            char c = globPattern.charAt(i);

            if (c == '\\' && i + 1 < len) {
                // Escaped character -> treat next char as literal
                char next = globPattern.charAt(i + 1);
                regex.append(Pattern.quote(String.valueOf(next)));
                i += 2;
                continue;
            }

            if (c == '(') {
                // Check for special (**/) or (*** /)
                if (globPattern.startsWith("**/)", i + 1)) {
                    regex.append("((?:[^/]+/)*)");
                    i += 5;
                    recursive = true;
                    continue;
                }
                if (globPattern.startsWith("***/)", i + 1)) {
                    regex.append("((?:[^/]+/)*)");
                    i += 6;
                    recursive = true;
                    continue;
                }
                regex.append('(');
                i++;
                continue;
            }

            if (c == ')') {
                regex.append(')');
                i++;
                continue;
            }

            if (globPattern.startsWith("**/", i)) {
                regex.append("(?:[^/]+/)*");
                i += 3;
                recursive = true;
                continue;
            }

            if (globPattern.startsWith("**", i)) {
                regex.append(".*");
                i += 2;
                recursive = true;
                continue;
            }

            if (c == '*') {
                regex.append("[^/]*");
                i++;
                continue;
            }

            if (c == '?') {
                regex.append("[^/]");
                i++;
                continue;
            }

            if (c == '[') {
                // Parse bracket expression
                int closeIndex = findClosingBracket(globPattern, i);
                if (closeIndex != -1) {
                    String bracketContent = globPattern.substring(i, closeIndex + 1);
                    regex.append(convertBracketExpression(bracketContent));
                    i = closeIndex + 1;
                    continue;
                }
            }

            if (c == '<') {
                // Number range like <1-10> or <->
                int closeIndex = globPattern.indexOf('>', i);
                if (closeIndex != -1) {
                    String rangeContent = globPattern.substring(i + 1, closeIndex);
                    if (rangeContent.equals("-")) {
                        regex.append("\\d+");
                        i = closeIndex + 1;
                        continue;
                    } else if (rangeContent.matches("\\d+-\\d+")) {
                        // For general matching of numeric ranges in filenames, match digits
                        regex.append("\\d+");
                        i = closeIndex + 1;
                        continue;
                    }
                }
            }

            if (c == '/') {
                regex.append("/");
                i++;
                continue;
            }

            // Other characters
            if ("^$.+{}|[]".indexOf(c) != -1) {
                regex.append('\\').append(c);
            } else {
                regex.append(c);
            }
            i++;
        }

        regex.append("$");
        Pattern pattern = Pattern.compile(regex.toString());
        return new CompiledPattern(pattern, baseDir, recursive, globPattern);
    }

    private static int findClosingBracket(String s, int startIndex) {
        int i = startIndex + 1;
        int len = s.length();
        if (i < len && (s.charAt(i) == '!' || s.charAt(i) == '^')) {
            i++;
        }
        if (i < len && s.charAt(i) == ']') {
            i++;
        }
        while (i < len) {
            if (s.charAt(i) == '\\' && i + 1 < len) {
                i += 2;
                continue;
            }
            if (s.startsWith("[:", i)) {
                int pos = s.indexOf(":]", i + 2);
                if (pos != -1) {
                    i = pos + 2;
                    continue;
                }
            }
            if (s.charAt(i) == ']') {
                return i;
            }
            i++;
        }
        return -1;
    }

    private static String convertBracketExpression(String bracket) {
        String content = bracket.substring(1, bracket.length() - 1);
        boolean negate = false;
        if (content.startsWith("!") || content.startsWith("^")) {
            negate = true;
            content = content.substring(1);
        }

        // Replace POSIX classes with Java character classes
        content = content.replace("[:upper:]", "\\p{Upper}")
                .replace("[:lower:]", "\\p{Lower}")
                .replace("[:alpha:]", "\\p{Alpha}")
                .replace("[:digit:]", "\\p{Digit}")
                .replace("[:alnum:]", "\\p{Alnum}")
                .replace("[:space:]", "\\s")
                .replace("[:blank:]", "\\p{Blank}")
                .replace("[:word:]", "\\w")
                .replace("[:xdigit:]", "\\p{XDigit}");

        return "[" + (negate ? "^" : "") + content + "&&[^/]]";
    }

    private static Path extractBaseDir(String pattern) {
        int firstWildcard = -1;
        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            if (c == '*' || c == '?' || c == '[' || c == '(' || c == '<') {
                firstWildcard = i;
                break;
            }
        }

        if (firstWildcard == -1) {
            // No wildcards
            int lastSlash = pattern.lastIndexOf('/');
            if (lastSlash != -1) {
                String dir = pattern.substring(0, lastSlash);
                return Paths.get(dir.isEmpty() ? "/" : dir);
            }
            return Paths.get(".");
        }

        String prefix = pattern.substring(0, firstWildcard);
        int lastSlash = prefix.lastIndexOf('/');
        if (lastSlash != -1) {
            String dir = prefix.substring(0, lastSlash);
            return Paths.get(dir.isEmpty() ? "/" : dir);
        }
        return Paths.get(".");
    }
}
