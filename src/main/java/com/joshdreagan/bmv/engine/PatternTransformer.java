package com.joshdreagan.bmv.engine;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PatternTransformer {

    private static final Pattern WILDCARD_PATTERN = Pattern.compile(
      "(\\*\\*+/|\\*+|\\?+|<\\d*-\\d*>|\\[(?:\\[:[a-z]+:]|\\\\.|]|[^]])+])"
    );

    public record TransformResult(String searchPattern, String replacementPattern, int searchWildcardCount, int replacementWildcardCount) {
    }

    public static TransformResult transform(String searchPattern, String replacementPattern, boolean optW, boolean optBigW) {
        if (!optW && !optBigW) {
            return new TransformResult(searchPattern, replacementPattern, 0, 0);
        }

        StringBuilder newSearch = new StringBuilder();
        int searchWildcards = 0;
        int i = 0;
        int len = searchPattern.length();

        while (i < len) {
            if (searchPattern.charAt(i) == '\\' && i + 1 < len) {
                newSearch.append(searchPattern.charAt(i)).append(searchPattern.charAt(i + 1));
                i += 2;
                continue;
            }

            Matcher m = WILDCARD_PATTERN.matcher(searchPattern);
            if (m.find(i) && m.start() == i) {
                String matched = m.group(1);
                newSearch.append('(').append(matched).append(')');
                searchWildcards++;
                i = m.end();
            } else {
                newSearch.append(searchPattern.charAt(i));
                i++;
            }
        }

        String finalRepl = replacementPattern;
        int replWildcards = 0;

        if (optBigW && replacementPattern != null) {
            StringBuilder newRepl = new StringBuilder();
            i = 0;
            len = replacementPattern.length();
            while (i < len) {
                if (replacementPattern.charAt(i) == '\\' && i + 1 < len) {
                    newRepl.append(replacementPattern.charAt(i)).append(replacementPattern.charAt(i + 1));
                    i += 2;
                    continue;
                }

                Matcher m = WILDCARD_PATTERN.matcher(replacementPattern);
                if (m.find(i) && m.start() == i) {
                    replWildcards++;
                    newRepl.append("${").append(replWildcards).append("}");
                    i = m.end();
                } else {
                    newRepl.append(replacementPattern.charAt(i));
                    i++;
                }
            }
            finalRepl = newRepl.toString();
        }

        return new TransformResult(newSearch.toString(), finalRepl, searchWildcards, replWildcards);
    }
}
