package sm.selflearn.samskrtam.content.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import sm.selflearn.samskrtam.common.SamskrtamException;
import sm.selflearn.samskrtam.content.dto.QuestionResponse;
import sm.selflearn.samskrtam.content.model.*;
import sm.selflearn.samskrtam.content.repository.DeclensionFormRepository;
import sm.selflearn.samskrtam.content.repository.DeclensionStemRepository;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeclensionQuizGeneratorService {

    private final DeclensionStemRepository declensionStemRepository;
    private final DeclensionFormRepository declensionFormRepository;

    private static final Random random = new Random();

    public List<QuestionResponse> generateDeclensionQuestions(Lesson lesson, Locale locale) {
        List<DeclensionStem> availableStems;

        // Определяем VowelType из slug урока
        VowelType vowelType = mapSlugToVowelType(lesson.getSlug());

        if (vowelType != null) {
            // Конкретный тип основы — фильтруем по vowelType
            availableStems = declensionStemRepository.findAll().stream()
                    .filter(stem -> stem.getVowelType() == vowelType)
                    .collect(Collectors.toList());
        } else {
            // slug не содержит конкретной основы (declensions-all) — берём все
            availableStems = declensionStemRepository.findAll();
        }

        if (availableStems.isEmpty()) {
            throw new SamskrtamException("NO_DECLENSION_STEMS", "No declension stems found for quiz type: " + lesson.getLessonType());
        }

        int questionsToGenerate = Math.min(lesson.getQuestionsPerSession(), availableStems.size());
        Collections.shuffle(availableStems);
        List<DeclensionStem> selectedStems = availableStems.subList(0, questionsToGenerate);

        List<QuestionResponse> generatedQuestions = new ArrayList<>();
        for (int i = 0; i < selectedStems.size(); i++) {
            DeclensionStem stem = selectedStems.get(i);
            generatedQuestions.add(generateSingleQuestion(stem, locale, i + 1));
        }
        return generatedQuestions;
    }

    private QuestionResponse generateSingleQuestion(DeclensionStem stem, Locale locale, int questionNumber) {
        CaseType targetCase = CaseType.values()[random.nextInt(CaseType.values().length)];
        NumberType targetNumber = NumberType.values()[random.nextInt(NumberType.values().length)];

        DeclensionForm correctForm = declensionFormRepository
                .findByDeclensionStemIdAndCaseTypeAndNumberType(stem.getId(), targetCase, targetNumber)
                .orElseThrow(() -> new SamskrtamException("DECLENSION_FORM_NOT_FOUND",
                        "Declension form not found for stem: " + stem.getStemIast() +
                                ", case: " + targetCase + ", number: " + targetNumber));

        String questionText = String.format(
                locale.getLanguage().equals("ru") ? "Основа: %s" : "Stem: %s",
                stem.getStemIast()
        );

        String explanationTextRu = String.format(
                "Правильная форма для основы '%s' в падеже '%s' и числе '%s' - '%s'.",
                stem.getStemIast(),
                targetCase.getRuName(),
                targetNumber.getRuName(),
                correctForm.getFormIast()
        );

        String explanationTextEn = String.format(
                "The correct form for stem '%s' in case '%s' and number '%s' is '%s'.",
                stem.getStemIast(),
                targetCase.getEnName(),
                targetNumber.getEnName(),
                correctForm.getFormIast()
        );

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
                .build();
    }

    private VowelType mapSlugToVowelType(String slug) {
        if (slug == null) return null;
        if (slug.startsWith("declensions-a-"))  return VowelType.A_STEM;
        if (slug.startsWith("declensions-aa-")) return VowelType.AA_STEM;
        if (slug.startsWith("declensions-ii-") || slug.equals("declensions-ii")) return VowelType.II_STEM;
        if (slug.startsWith("declensions-i-")  || slug.equals("declensions-i"))  return VowelType.I_STEM;
        if (slug.startsWith("declensions-uu-") || slug.equals("declensions-uu")) return VowelType.UU_STEM;
        if (slug.startsWith("declensions-u-")  || slug.equals("declensions-u"))  return VowelType.U_STEM;
        if (slug.startsWith("declensions-r-")  || slug.equals("declensions-r"))  return VowelType.R_STEM;
        return null; // declensions-all или неизвестный slug → все основы
    }}

