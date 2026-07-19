package sm.selflearn.samskrtam.quiz.service;

import sm.selflearn.samskrtam.quiz.model.FilterCombination;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JSON helper methods for quiz filter canonicalization.
 * Extracted from QuizSessionService for cohesion.
 */
public final class QuizFilterJsonHelper {

    private QuizFilterJsonHelper() { }

    /** Builds a canonical sorted JSON array from a list of strings for set equality comparison. */
    public static String buildCanonicalJsonArray(List<String> items) {
        if (items == null || items.isEmpty()) return null;
        List<String> sorted = new ArrayList<>(items);
        sorted.sort(String::compareTo);
        return "[" + sorted.stream()
                .map(s -> "\"" + escapeJson(s) + "\"")
                .collect(Collectors.joining(",")) + "]";
    }

    /** Builds a canonical sorted JSON array of {caseType,numberType,gender} objects. */
    public static String buildCanonicalCombinationsJson(List<FilterCombination> combinations) {
        if (combinations == null || combinations.isEmpty()) return null;
        List<FilterCombination> sorted = new ArrayList<>(combinations);
        sorted.sort(FilterCombination::compareTo);
        return "[" + sorted.stream()
                .map(c -> "{\"caseType\":\"" + escapeJson(c.caseType()) + "\"," +
                          "\"numberType\":\"" + escapeJson(c.numberType()) + "\"," +
                          "\"gender\":\"" + escapeJson(c.gender()) + "\"}")
                .collect(Collectors.joining(",")) + "]";
    }

    public static List<String> parseCsvToList(String csv) {
        if (csv == null || csv.isBlank()) return List.of();
        return List.of(csv.split(","));
    }

    /**
     * Parses a JSON array string like {@code ["NOMINATIVE","ACCUSATIVE"]} into a list of strings.
     * Used by scope pre-filter to parse canonicalized filter values stored in QuizSession.
     */
    public static List<String> parseJsonArray(String jsonArray) {
        if (jsonArray == null || jsonArray.isBlank()) return List.of();
        // Strip brackets and extract quoted strings
        String inner = jsonArray.trim();
        if (inner.startsWith("[") && inner.endsWith("]")) {
            inner = inner.substring(1, inner.length() - 1).trim();
        }
        if (inner.isEmpty()) return List.of();
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < inner.length(); i++) {
            char ch = inner.charAt(i);
            if (ch == '"' && (i == 0 || inner.charAt(i - 1) != '\\')) {
                inQuotes = !inQuotes;
            } else if (ch == ',' && !inQuotes) {
                result.add(current.toString().trim());
                current.setLength(0);
            } else if (inQuotes) {
                current.append(ch);
            }
        }
        if (current.length() > 0) {
            result.add(current.toString().trim());
        }
        return result.stream().filter(s -> !s.isEmpty()).collect(Collectors.toList());
    }

    /** Parses comma-separated "caseType:numberType:gender" triples. */
    public static List<FilterCombination> parseCombinations(String csv) {
        if (csv == null || csv.isBlank()) return List.of();
        List<FilterCombination> result = new ArrayList<>();
        for (String part : csv.split(",")) {
            String[] fields = part.split(":");
            if (fields.length >= 3) {
                result.add(new FilterCombination(fields[0].trim(), fields[1].trim(), fields[2].trim()));
            }
        }
        return result;
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
