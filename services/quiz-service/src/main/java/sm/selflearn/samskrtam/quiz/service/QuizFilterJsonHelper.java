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
