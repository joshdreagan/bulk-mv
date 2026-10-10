package com.joshdreagan.bmv.engine;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReplacementProcessor {

    private static final Pattern VAR_PATTERN = Pattern.compile(
      "\\$(?:(\\d+)|\\{([^}]+)}|([a-zA-Z_][a-zA-Z0-9_]*))"
    );

    public static String evaluate(String replacementPattern, String matchedFile, List<String> captureGroups) {
        StringBuilder result = new StringBuilder();
        int i = 0;
        int len = replacementPattern.length();

        while (i < len) {
            char c = replacementPattern.charAt(i);

            if (c == '\\' && i + 1 < len) {
                result.append(replacementPattern.charAt(i + 1));
                i += 2;
                continue;
            }

            if (c == '$') {
                Matcher m = VAR_PATTERN.matcher(replacementPattern);
                if (m.find(i) && m.start() == i) {
                    String simpleNum = m.group(1);
                    String bracedExpr = m.group(2);
                    String simpleName = m.group(3);

                    String replacementValue = "";

                    if (simpleNum != null) {
                        int groupIndex = Integer.parseInt(simpleNum);
                        replacementValue = getGroupValue(groupIndex, captureGroups);
                    } else if (simpleName != null) {
                        if ("f".equals(simpleName)) {
                            replacementValue = matchedFile;
                        } else {
                            replacementValue = "";
                        }
                    } else if (bracedExpr != null) {
                        replacementValue = evaluateBracedExpression(bracedExpr, matchedFile, captureGroups);
                    }

                    result.append(replacementValue);
                    i = m.end();
                    continue;
                }
            }

            result.append(c);
            i++;
        }

        return result.toString();
    }

    private static String getGroupValue(int index, List<String> captureGroups) {
        if (index > 0 && index <= captureGroups.size()) {
            String val = captureGroups.get(index - 1);
            return val != null ? val : "";
        }
        return "";
    }

    private static String evaluateBracedExpression(String expr, String matchedFile, List<String> captureGroups) {
        String work = expr.trim();
        String flags = "";

        // Check for flags like (L), (U), (C)
        if (work.startsWith("(") && work.contains(")")) {
            int closeParen = work.indexOf(')');
            flags = work.substring(1, closeParen);
            work = work.substring(closeParen + 1).trim();
        }

        String varName = work;
        String modifiers = "";
        String searchPattern = null;
        String replaceStr = null;
        boolean replaceAll = false;

        // Check for substitution //pattern/replacement or /pattern/replacement
        if (work.contains("//")) {
            int slashSlash = work.indexOf("//");
            varName = work.substring(0, slashSlash);
            String rest = work.substring(slashSlash + 2);
            int nextSlash = rest.indexOf('/');
            if (nextSlash != -1) {
                searchPattern = rest.substring(0, nextSlash);
                replaceStr = rest.substring(nextSlash + 1);
            } else {
                searchPattern = rest;
                replaceStr = "";
            }
            replaceAll = true;
        } else if (work.contains("/") && !work.startsWith(":s/")) {
            int slash = work.indexOf('/');
            varName = work.substring(0, slash);
            String rest = work.substring(slash + 1);
            int nextSlash = rest.indexOf('/');
            if (nextSlash != -1) {
                searchPattern = rest.substring(0, nextSlash);
                replaceStr = rest.substring(nextSlash + 1);
            } else {
                searchPattern = rest;
                replaceStr = "";
            }
        } else if (work.contains(":")) {
            int colon = work.indexOf(':');
            varName = work.substring(0, colon);
            modifiers = work.substring(colon);
        }

        // Get initial value of var
        String val = "";
        if (varName.matches("\\d+")) {
            val = getGroupValue(Integer.parseInt(varName), captureGroups);
        } else if ("f".equals(varName)) {
            val = matchedFile;
        }

        // Apply substitution if present
        if (searchPattern != null) {
            if (replaceAll) {
                val = val.replace(searchPattern, replaceStr);
            } else {
                int idx = val.indexOf(searchPattern);
                if (idx != -1) {
                    val = val.substring(0, idx) + replaceStr + val.substring(idx + searchPattern.length());
                }
            }
        }

        // Apply modifiers like :r, :e, :h, :t
        if (!modifiers.isEmpty()) {
            val = applyModifiers(val, modifiers);
        }

        // Apply flags
        if (flags.contains("L")) {
            val = val.toLowerCase();
        } else if (flags.contains("U")) {
            val = val.toUpperCase();
        } else if (flags.contains("C")) {
            val = capitalize(val);
        }

        return val;
    }

    private static String applyModifiers(String val, String modifiers) {
        String[] mods = modifiers.split("(?=:)");
        String current = val;

        for (String mod : mods) {
            if (mod.isEmpty()) continue;
            switch (mod) {
                case ":h" -> { // Head (dirname)
                    int lastSlash = current.lastIndexOf('/');
                    current = (lastSlash != -1) ? current.substring(0, lastSlash) : ".";
                }
                case ":t" -> { // Tail (basename)
                    int lastSlash = current.lastIndexOf('/');
                    current = (lastSlash != -1) ? current.substring(lastSlash + 1) : current;
                }
                case ":r" -> { // Root (remove extension)
                    int lastDot = current.lastIndexOf('.');
                    int lastSlash = current.lastIndexOf('/');
                    if (lastDot > lastSlash) {
                        current = current.substring(0, lastDot);
                    }
                }
                case ":e" -> { // Extension
                    int lastDot = current.lastIndexOf('.');
                    int lastSlash = current.lastIndexOf('/');
                    if (lastDot > lastSlash) {
                        current = current.substring(lastDot + 1);
                    } else {
                        current = "";
                    }
                }
                case ":u" -> current = current.toUpperCase();
                case ":l" -> current = current.toLowerCase();
                default -> {
                    // Check if modifier is :s/old/new/ or :gs/old/new/
                    if (mod.startsWith(":s/") || mod.startsWith(":gs/")) {
                        boolean all = mod.startsWith(":gs/");
                        String sRest = mod.substring(all ? 4 : 3);
                        String[] parts = sRest.split("/", -1);
                        if (parts.length >= 2) {
                            String oldStr = parts[0];
                            String newStr = parts[1];
                            if (all) {
                                current = current.replace(oldStr, newStr);
                            } else {
                                current = current.replaceFirst(Pattern.quote(oldStr), Matcher.quoteReplacement(newStr));
                            }
                        }
                    }
                }
            }
        }
        return current;
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        char[] chars = s.toCharArray();
        boolean newWord = true;
        for (int i = 0; i < chars.length; i++) {
            if (Character.isWhitespace(chars[i]) || chars[i] == '_' || chars[i] == '-' || chars[i] == '.') {
                newWord = true;
            } else if (newWord) {
                chars[i] = Character.toUpperCase(chars[i]);
                newWord = false;
            } else {
                chars[i] = Character.toLowerCase(chars[i]);
            }
        }
        return new String(chars);
    }
}
