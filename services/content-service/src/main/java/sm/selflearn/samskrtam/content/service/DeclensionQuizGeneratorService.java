package sm.selflearn.samskrtam.content.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import sm.selflearn.samskrtam.common.SamskrtamException;
import sm.selflearn.samskrtam.content.dto.QuestionResponse;
import sm.selflearn.samskrtam.content.model.*;
import sm.selflearn.samskrtam.content.repository.CaseEndingRepository;
import sm.selflearn.samskrtam.content.repository.DeclensionFormRepository;
import sm.selflearn.samskrtam.content.repository.DeclensionStemRepository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeclensionQuizGeneratorService {

        private final DeclensionStemRepository declensionStemRepository;
        private final DeclensionFormRepository declensionFormRepository;
        private final CaseEndingRepository caseEndingRepository;
        private final ObjectMapper objectMapper;

        private static final Random random = new Random();

        public List<QuestionResponse> generateDeclensionQuestions(Lesson lesson, Locale locale,
                                                                      String filterVowelTypes, String filterGenders) {
            List<DeclensionStem> availableStems;

            // Определяем VowelType(s) из slug урока (поддерживает составные уроки: declensions-i-u, declensions-ii-uu)
            List<VowelType> vowelTypes = SlugToVowelTypeMapper.mapSlugToVowelTypes(lesson.getSlug());

            if (!vowelTypes.isEmpty()) {
                // Конкретные типы основ — фильтруем по списку vowelType
                availableStems = declensionStemRepository.findByVowelTypeIn(vowelTypes);
            } else if ("declensions-all".equals(lesson.getSlug())) {
                // ALL_STEMS: кросс-lesson выборка по filterVowelTypes + filterGenders (из query-параметров)
                List<VowelType> filterVt = parseVowelTypes(filterVowelTypes);
                List<Gender> filterG = parseGenders(filterGenders);
                availableStems = declensionStemRepository.findByVowelTypeInAndGenderIn(
                        filterVt.isEmpty() ? null : filterVt,
                        filterG.isEmpty() ? null : filterG);
            } else {
                // slug не содержит конкретной основы — берём все
                availableStems = declensionStemRepository.findAll();
            }

        if (availableStems.isEmpty()) {
            throw new SamskrtamException("NO_DECLENSION_STEMS", "No declension stems found for quiz type: " + lesson.getLessonType());
        }

        int questionsToGenerate = Math.min(lesson.getQuestionsPerSession(), availableStems.size());
        Collections.shuffle(availableStems);
        //List<DeclensionStem> selectedStems = availableStems.subList(0, questionsToGenerate);
        List<DeclensionStem> selectedStems = availableStems.stream().toList();

        // Track previously assigned question types in this session for anti-repeat logic
        List<String> previousQuestionTypesInSession = new ArrayList<>();

        List<QuestionResponse> generatedQuestions = new ArrayList<>();
        for (int i = 0; i < selectedStems.size(); i++) {
            DeclensionStem stem = selectedStems.get(i);
            generatedQuestions.add(generateSingleQuestion(stem, locale, i + 1, previousQuestionTypesInSession));
        }
        return generatedQuestions;
    }

        private QuestionResponse generateSingleQuestion(DeclensionStem stem, Locale locale, int questionNumber,
                                                     List<String> previousQuestionTypesInSession) {
        CaseType targetCase = CaseType.values()[random.nextInt(CaseType.values().length)];
        NumberType targetNumber = NumberType.values()[random.nextInt(NumberType.values().length)];

        DeclensionForm correctForm = declensionFormRepository
                .findByDeclensionStemIdAndCaseTypeAndNumberType(stem.getId(), targetCase, targetNumber)
                .orElseThrow(() -> new SamskrtamException("DECLENSION_FORM_NOT_FOUND",
                        "Declension form not found for stem: " + stem.getStemIast() +
                                ", case: " + targetCase + ", number: " + targetNumber));

        // Определяем CaseEnding для получения endingIast и caseEndingId
        CaseEnding resolvedEnding = resolveCaseEnding(stem.getVowelType(), stem.getGender(), targetCase, targetNumber);
        UUID caseEndingId = resolvedEnding != null ? resolvedEnding.getId() : null;
        String endingIast = resolvedEnding != null ? resolvedEnding.getEndingIast() : null;

        // Проверяем, достаточно ли омонимов у этого окончания (≥2 троек с тем же endingIast в рамках vowelType)
        boolean endingHasEnoughHomonyms = endingIast != null
                && caseEndingRepository.findByVowelTypeAndEndingIast(stem.getVowelType(), endingIast).size() >= 2;

        // Назначаем questionType
        String questionType = resolveQuestionType(previousQuestionTypesInSession, endingHasEnoughHomonyms);
        previousQuestionTypesInSession.add(questionType);

        String questionText = String.format(
                locale.getLanguage().equals("ru") ? "Основа: %s" : "Stem: %s",
                stem.getStemIast()
        );

                String explanationTextRu;
        String explanationTextEn;
        if ("CASE_BY_FORM".equals(questionType)) {
            explanationTextRu = String.format(
                    "Форма '%s' основы '%s' стоит в падеже '%s', числе '%s'.",
                    correctForm.getFormIast(),
                    stem.getStemIast(),
                    targetCase.getRuName(),
                    targetNumber.getRuName()
            );
            explanationTextEn = String.format(
                    "The form '%s' of stem '%s' is in case '%s', number '%s'.",
                    correctForm.getFormIast(),
                    stem.getStemIast(),
                    targetCase.getEnName(),
                    targetNumber.getEnName()
            );
        } else if ("ENDING_MATCH".equals(questionType)) {
            explanationTextRu = String.format(
                    "Окончание '%s' в данном типе основы соответствует падежу '%s', числу '%s' (и может совпадать с другими падежами/числами).",
                    endingIast != null ? endingIast : "",
                    targetCase.getRuName(),
                    targetNumber.getRuName()
            );
            explanationTextEn = String.format(
                    "The ending '%s' for this stem type corresponds to case '%s', number '%s' (and may coincide with other case/number combinations).",
                    endingIast != null ? endingIast : "",
                    targetCase.getEnName(),
                    targetNumber.getEnName()
            );
        } else {
            explanationTextRu = String.format(
                    "Правильная форма для основы '%s' в падеже '%s' и числе '%s' - '%s'.",
                    stem.getStemIast(),
                    targetCase.getRuName(),
                    targetNumber.getRuName(),
                    correctForm.getFormIast()
            );
            explanationTextEn = String.format(
                    "The correct form for stem '%s' in case '%s' and number '%s' is '%s'.",
                    stem.getStemIast(),
                    targetCase.getEnName(),
                    targetNumber.getEnName(),
                    correctForm.getFormIast()
            );
        }

        return QuestionResponse.builder()
                .id(UUID.randomUUID())
                .questionNumber(questionNumber)
                .text(questionText)
                .explanationRu(explanationTextRu)
                .explanationEn(explanationTextEn)
                .declensionStemId(stem.getId())
                .targetCase(targetCase)
                .targetNumber(targetNumber)
                .correctFormIast(correctForm.getFormIast())
                .correctFormDevanagari(correctForm.getFormDevanagari())
                .stem(stem.getStemIast())
                .stemDevanagari(stem.getStemDevanagari())
                .stemTranslationRu(stem.getTranslationRu())
                .stemTranslationEn(stem.getTranslationEn())
                .gender(stem.getGender() != null ? stem.getGender().name() : null)
                .caseEndingId(caseEndingId)
                .questionType(questionType)
                .caseEnding(endingIast)
                .build();
    }

        /**
     * Назначает questionType для текущего вопроса на основе истории и доступности ENDING_MATCH.
     *
     * @param previousQuestionTypesInSession список уже назначенных questionType в этой сессии (по порядку questionNumber)
     * @param endingHasEnoughHomonyms        true если окончание имеет ≥2 омонимичных троек (vowel_type, endingIast)
     * @return один из "FORM_BY_CASE", "CASE_BY_FORM", "ENDING_MATCH"
     */
    String resolveQuestionType(List<String> previousQuestionTypesInSession, boolean endingHasEnoughHomonyms) {
        // Собираем пул кандидатов (дубликаты = "вес")
        List<String> pool = new ArrayList<>(List.of(
                "FORM_BY_CASE", "FORM_BY_CASE", "FORM_BY_CASE", "FORM_BY_CASE", "FORM_BY_CASE",
                "CASE_BY_FORM", "CASE_BY_FORM", "CASE_BY_FORM"
        ));
        //if (endingHasEnoughHomonyms) {
            //pool.add("ENDING_MATCH");
            //pool.add("ENDING_MATCH");
        //} else {
            pool.add("CASE_BY_FORM");
            pool.add("CASE_BY_FORM");
        //}

        // Определяем lastTwo — последние 2 элемента истории
        List<String> lastTwo = previousQuestionTypesInSession.size() >= 2
                ? previousQuestionTypesInSession.subList(previousQuestionTypesInSession.size() - 2, previousQuestionTypesInSession.size())
                : List.of();

        // До 10 попыток — антиповтор двух одинаковых подряд
        String candidate = null;
        for (int attempt = 0; attempt < 10; attempt++) {
            candidate = pool.get(random.nextInt(pool.size()));
            boolean isRepeat = lastTwo.size() == 2
                    && lastTwo.get(0).equals(candidate)
                    && lastTwo.get(1).equals(candidate);
            if (!isRepeat) {
                return candidate;
            }
        }
        // Крайний случай — не зацикливаемся
        return candidate;
    }

    /**
     * Определяет CaseEnding для (vowelType, gender, caseType, numberType).
     * Для основ -i, -u, -ṛ ищет UNSPECIFIED gender.
     *
     * @return найденный CaseEnding или null
     */
    private CaseEnding resolveCaseEnding(VowelType vowelType, Gender gender, CaseType caseType, NumberType numberType) {
        if (gender == null) {
            gender = Gender.UNSPECIFIED;
        }
                boolean isUnspecifiedGenderType = (vowelType == VowelType.I_STEM
                || vowelType == VowelType.II_STEM
                || vowelType == VowelType.U_STEM
                || vowelType == VowelType.UU_STEM
                || vowelType == VowelType.R_STEM
                || vowelType == VowelType.PRON_AHAM
                || vowelType == VowelType.PRON_TVAM);

        if (isUnspecifiedGenderType) {
            var endings = caseEndingRepository.findByVowelTypeAndGenderAndCaseTypeAndNumberType(
                    vowelType, Gender.UNSPECIFIED, caseType, numberType);
            if (!endings.isEmpty()) {
                return endings.get(0);
            }
        }

        var endings = caseEndingRepository.findByVowelTypeAndGenderAndCaseTypeAndNumberType(
                vowelType, gender, caseType, numberType);
        if (!endings.isEmpty()) {
            return endings.get(0);
        }

        log.warn("Case ending not found for vowelType={}, gender={}, caseType={}, numberType={}",
                vowelType, gender, caseType, numberType);
        return null;
    }

        /**
         * Parses filterVowelTypes: JSON array like ["A_STEM","AA_STEM"] — primary format from quiz-service,
         * with CSV fallback for backward compatibility.
         */
        private List<VowelType> parseVowelTypes(String filterVowelTypes) {
            return parseEnumList(filterVowelTypes, VowelType.class);
        }

        /**
         * Parses filterGenders: JSON array like ["MASCULINE","FEMININE"] — primary format from quiz-service,
         * with CSV fallback for backward compatibility.
         */
        private List<Gender> parseGenders(String filterGenders) {
            return parseEnumList(filterGenders, Gender.class);
        }

        private <E extends Enum<E>> List<E> parseEnumList(String raw, Class<E> enumClass) {
            if (raw == null || raw.isBlank()) return List.of();
            List<String> tokens = parseStringList(raw);
            List<E> result = new ArrayList<>();
            for (String token : tokens) {
                try {
                    result.add(Enum.valueOf(enumClass, token.trim()));
                } catch (IllegalArgumentException e) {
                    // ignore unknown values
                }
            }
            return result;
        }

        /**
         * Primary: JSON array like {@code ["A_STEM","AA_STEM"]}.
         * Fallback: comma-separated values like {@code "A_STEM,AA_STEM"}.
         */
        private List<String> parseStringList(String raw) {
            String trimmed = raw.trim();
            if (trimmed.startsWith("[")) {
                try {
                    return objectMapper.readValue(trimmed, new TypeReference<List<String>>() {});
                } catch (Exception e) {
                    log.debug("Failed to parse as JSON array, falling back to CSV: {}", trimmed, e);
                }
            }
            return List.of(trimmed.split(","));
        }
}

