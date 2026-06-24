package sm.selflearn.samskrtam.content.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import sm.selflearn.samskrtam.common.SamskrtamException;
import sm.selflearn.samskrtam.content.dto.QuestionResponse;
import sm.selflearn.samskrtam.content.dto.LessonType;
import sm.selflearn.samskrtam.content.model.Case; // Corrected import for Case
import sm.selflearn.samskrtam.content.model.DeclensionForm;
import sm.selflearn.samskrtam.content.model.DeclensionStem;
import sm.selflearn.samskrtam.content.model.Lesson;
import sm.selflearn.samskrtam.content.model.VowelType;
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

        if (lesson.getLessonType().toString().contains("DECLENSIONS")) {
            VowelType requiredVowelType = mapLessonTypeToVowelType(lesson.getLessonType());
            availableStems = declensionStemRepository.findAll().stream()
                    .filter(stem -> stem.getVowelType() == requiredVowelType)
                    .collect(Collectors.toList());
        } else {
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
        Case targetCase = Case.values()[random.nextInt(Case.values().length)];
        // Assuming Number is in the same package as Case
        sm.selflearn.samskrtam.content.model.Number targetNumber = sm.selflearn.samskrtam.content.model.Number.values()[random.nextInt(sm.selflearn.samskrtam.content.model.Number.values().length)];

        DeclensionForm correctForm = declensionFormRepository
                .findByDeclensionStemIdAndCaseTypeAndNumberType(stem.getId(), targetCase, targetNumber)
                .orElseThrow(() -> new SamskrtamException("DECLENSION_FORM_NOT_FOUND",
                        "Declension form not found for stem: " + stem.getStemNameIast() +
                                ", case: " + targetCase + ", number: " + targetNumber));

        String questionText = String.format(
                locale.getLanguage().equals("ru") ? "Основа: %s, Падеж: %s, Число: %s" : "Stem: %s, Case: %s, Number: %s",
                stem.getStemNameIast(),
                locale.getLanguage().equals("ru") ? targetCase.getRuName() : targetCase.getEnName(),
                locale.getLanguage().equals("ru") ? targetNumber.getRuName() : targetNumber.getEnName()
        );

        String explanationTextRu = String.format(
                "Правильная форма для основы '%s' в падеже '%s' и числе '%s' - '%s'.",
                stem.getStemNameIast(),
                targetCase.getRuName(),
                targetNumber.getRuName(),
                correctForm.getFormIast()
        );

        String explanationTextEn = String.format(
                "The correct form for stem '%s' in case '%s' and number '%s' is '%s'.",
                stem.getStemNameIast(),
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
                .stem(stem.getStemNameIast())
                .build();
    }

    private VowelType mapLessonTypeToVowelType(LessonType lessonType) {
        return switch (lessonType) {
            case A_STEM_DECLENSIONS -> VowelType.A_STEM;
            case AA_STEM_DECLENSIONS -> VowelType.AA_STEM;
            case I_STEM_DECLENSIONS -> VowelType.I_STEM;
            case II_STEM_DECLENSIONS -> VowelType.II_STEM;
            case U_STEM_DECLENSIONS -> VowelType.U_STEM;
            case UU_STEM_DECLENSIONS -> VowelType.UU_STEM;
            case R_STEM_DECLENSIONS -> VowelType.R_STEM;
            default -> throw new SamskrtamException("UNSUPPORTED_QUIZ_TYPE", "Quiz type " + lessonType + " is not a specific declension type.");
        };
    }
}
