package sm.selflearn.samskrtam.content.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import sm.selflearn.samskrtam.content.dto.GeneratedQuizQuestionDto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Pre-filters questions by scope parameters BEFORE SRS selection.
 * <p>
 * Extracted from quiz-service {@code SessionCreationService.applyScopeFilter}
 * as a self-contained Spring bean in content-service.
 * Uses plain {@code String filterScope} instead of quiz-service's {@code FilterScope} enum.
 * </p>
 *
 * @see sm.selflearn.samskrtam.content.dto.GeneratedQuizQuestionDto
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QuizScopeFilterService {

    /**
     * Local triple for filter combinations: (caseType, numberType, gender).
     * Mirrors quiz-service's {@code FilterCombination} without cross-service dependency.
     */
    private record FilterCombination(String caseType, String numberType, String gender) {
    }

    /**
     * Pre-filters questions by scope parameters.
     *
     * @param questions          full list of generated questions
          * @param filterScope        {@code "CASE_ONLY"} / {@code "NUMBER_ONLY"} / {@code "CASE_NUMBER_GENDER"} / {@code "ALL_STEMS"}; {@code null} or blank → no filtering
     * @param filterCaseTypes    JSON array of allowed {@code CaseType} names (for {@code CASE_ONLY})
     * @param filterNumberTypes  JSON array of allowed {@code NumberType} names (for {@code NUMBER_ONLY} / {@code ALL_STEMS})
     * @param filterCombinations JSON array of {@code {caseType,numberType,gender}} objects (for {@code CASE_NUMBER_GENDER})
     * @return filtered list (may be empty); the original list if {@code filterScope} is blank
     */
    public List<GeneratedQuizQuestionDto> filterQuestions(
            List<GeneratedQuizQuestionDto> questions,
            String filterScope,
            String filterCaseTypes,
            String filterNumberTypes,
            String filterCombinations) {

        if (filterScope == null || filterScope.isBlank()) {
            return questions;
        }

        return switch (filterScope) {
            case "CASE_ONLY" -> {
                Set<String> allowedCases = filterCaseTypes != null
                        ? new HashSet<>(parseJsonArray(filterCaseTypes))
                        : Collections.emptySet();
                yield questions.stream()
                        .filter(q -> q.getTargetCase() != null
                                && (allowedCases.isEmpty() || allowedCases.contains(q.getTargetCase().name())))
                        .collect(Collectors.toList());
            }
                        case "NUMBER_ONLY" -> {
                            Set<String> allowedNumbers = resolveAllowedNumbers(filterNumberTypes);
                            yield questions.stream()
                                    .filter(q -> q.getTargetNumber() != null
                                            && allowedNumbers.contains(q.getTargetNumber().name()))
                                    .collect(Collectors.toList());
                        }
                        case "ALL_STEMS" -> {
                            // ALL_STEMS: vowelType and gender are already pre-filtered at the DB level
                            // (DeclensionQuizGeneratorService). Here we filter by numberType and caseType.
                            Set<String> allowedNumbers = resolveAllowedNumbers(filterNumberTypes);
                            Set<String> allowedCases = filterCaseTypes != null
                                    ? new HashSet<>(parseJsonArray(filterCaseTypes))
                                    : Collections.emptySet();
                            yield questions.stream()
                                    .filter(q -> q.getTargetNumber() != null
                                            && allowedNumbers.contains(q.getTargetNumber().name()))
                                    .filter(q -> allowedCases.isEmpty()
                                            || (q.getTargetCase() != null && allowedCases.contains(q.getTargetCase().name())))
                                    .collect(Collectors.toList());
                        }
            case "CASE_NUMBER_GENDER" -> {
                List<FilterCombination> combos = filterCombinations != null
                        ? parseCombinationsJson(filterCombinations)
                        : Collections.emptyList();
                if (combos.isEmpty()) {
                    yield questions;
                }
                Set<String> comboCodes = combos.stream()
                        .map(c -> c.caseType() + ":" + c.numberType() + ":" + c.gender())
                        .collect(Collectors.toSet());
                yield questions.stream()
                        .filter(q -> {
                            String caseStr = q.getTargetCase() != null ? q.getTargetCase().name() : "";
                            String numStr = q.getTargetNumber() != null ? q.getTargetNumber().name() : "";
                            String genStr = q.getGender() != null ? q.getGender() : "";
                            return comboCodes.contains(caseStr + ":" + numStr + ":" + genStr);
                        })
                        .collect(Collectors.toList());
            }
            default -> {
                log.warn("Unknown filterScope: '{}', returning unfiltered questions", filterScope);
                yield questions;
            }
        };
    }

    // ================== JSON parsing helpers ==================

    /**
     * Parses a JSON array of {@code {caseType,numberType,gender}} objects.
     * Example: {@code [{"caseType":"NOMINATIVE","numberType":"SINGULAR","gender":"MASCULINE"}]}
     */
    private List<FilterCombination> parseCombinationsJson(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        List<FilterCombination> result = new ArrayList<>();
        String inner = json.trim();
        if (inner.startsWith("[") && inner.endsWith("]")) {
            inner = inner.substring(1, inner.length() - 1).trim();
        }
        if (inner.isEmpty()) return result;
        // Split by "},{"
        String[] objects = inner.split("\\},\\{");
        for (String obj : objects) {
            obj = obj.replace("{", "").replace("}", "").trim();
            String caseType = extractJsonValue(obj, "caseType");
            String numberType = extractJsonValue(obj, "numberType");
            String gender = extractJsonValue(obj, "gender");
            if (caseType != null && numberType != null && gender != null) {
                result.add(new FilterCombination(caseType, numberType, gender));
            }
        }
        return result;
    }

    /**
     * Extracts a string value for the given key from a JSON object fragment.
     */
    private String extractJsonValue(String obj, String key) {
        String searchKey = "\"" + key + "\":\"";
        int start = obj.indexOf(searchKey);
        if (start < 0) return null;
        start += searchKey.length();
        int end = obj.indexOf("\"", start);
        if (end < 0) return null;
        return obj.substring(start, end);
    }

        /**
     * Resolves allowed number types: parses JSON array, expands empty to all three numbers.
     *
     * @param filterNumberTypes JSON array string or null/blank
     * @return non-empty set of NumberType names
     */
    private Set<String> resolveAllowedNumbers(String filterNumberTypes) {
        Set<String> result = filterNumberTypes != null
                ? new HashSet<>(parseJsonArray(filterNumberTypes))
                : new HashSet<>();
        if (result.isEmpty()) {
            result = new HashSet<>(List.of("SINGULAR", "DUAL", "PLURAL"));
        }
        return result;
    }

    /**
     * Parses a JSON array string like {@code ["NOMINATIVE","ACCUSATIVE"]} into a list of strings.
     */
    private List<String> parseJsonArray(String jsonArray) {
        if (jsonArray == null || jsonArray.isBlank()) return List.of();
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
}
