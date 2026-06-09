package sm.selflearn.samskrtam.content.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import sm.selflearn.samskrtam.common.SamskrtamException;
import sm.selflearn.samskrtam.content.dto.QuestionResponse;
import sm.selflearn.samskrtam.content.dto.QuizType;
import sm.selflearn.samskrtam.content.model.*;
import sm.selflearn.samskrtam.content.model.Number;
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

    public List<QuestionResponse> generateDeclensionQuestions(Quiz quiz, Locale locale) {
        List<DeclensionStem> availableStems;

        // Filter stems by vowel type if the quiz is specific (e.g., A_STEM quiz)
        if (quiz.getQuizType() != QuizType.DECLENSIONS) { // Assuming DECLENSIONS is the combined type
            VowelType requiredVowelType = mapQuizTypeToVowelType(quiz.getQuizType());
            availableStems = declensionStemRepository.findAll().stream()
                    .filter(stem -> stem.getVowelType() == requiredVowelType)
                    .collect(Collectors.toList());
        } else {
            // For combined DECLENSIONS quiz, use all available stems
            availableStems = declensionStemRepository.findAll();
        }

        if (availableStems.isEmpty()) {
            throw new SamskrtamException("NO_DECLENSION_STEMS", "No declension stems found for quiz type: " + quiz.getQuizType());
        }

        // Select N random stems for the session
        int questionsToGenerate = Math.min(quiz.getQuestionsPerSession(), availableStems.size());
        Collections.shuffle(availableStems);
        List<DeclensionStem> selectedStems = availableStems.subList(0, questionsToGenerate);

        List<QuestionResponse> generatedQuestions = new ArrayList<>();
        for (DeclensionStem stem : selectedStems) {
            generatedQuestions.add(generateSingleQuestion(stem, locale));
        }
        return generatedQuestions;
    }

    private QuestionResponse generateSingleQuestion(DeclensionStem stem, Locale locale) {
        // Randomly select a Case and Number for the target form
        Case targetCase = Case.values()[random.nextInt(Case.values().length)];
        Number targetNumber = Number.values()[random.nextInt(Number.values().length)];

        // Retrieve the correct form
        DeclensionForm correctForm = declensionFormRepository
                .findByDeclensionStemIdAndCaseTypeAndNumberType(stem.getId(), targetCase, targetNumber)
                .orElseThrow(() -> new SamskrtamException("DECLENSION_FORM_NOT_FOUND",
                        "Declension form not found for stem: " + stem.getStemNameIast() +
                                ", case: " + targetCase + ", number: " + targetNumber));

        String questionText = String.format(
                locale.getLanguage().equals("ru") ? "Основа: %s, Падеж: %s, Число: %s" : "Stem: %s, Case: %s, Number: %s",
                stem.getStemNameIast(),
                targetCase.getRuName(),
                targetNumber.getRuName()
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
                .id(UUID.randomUUID()) // Generate a new UUID for the question
                .text(questionText)
                .explanationRu(explanationTextRu) // Changed to explanationRu
                .explanationEn(explanationTextEn) // Added explanationEn
                .declensionStemId(stem.getId())
                .targetCase(targetCase)
                .targetNumber(targetNumber)
                .correctFormIast(correctForm.getFormIast())
                .correctFormDevanagari(correctForm.getFormDevanagari())
                .build();
    }

    private VowelType mapQuizTypeToVowelType(QuizType quizType) {
        return switch (quizType) {
            case A_STEM_DECLENSIONS -> VowelType.A_STEM;
            case AA_STEM_DECLENSIONS -> VowelType.AA_STEM;
            case I_STEM_DECLENSIONS -> VowelType.I_STEM;
            case II_STEM_DECLENSIONS -> VowelType.II_STEM;
            case U_STEM_DECLENSIONS -> VowelType.U_STEM;
            case UU_STEM_DECLENSIONS -> VowelType.UU_STEM;
            case R_STEM_DECLENSIONS -> VowelType.R_STEM;
            default -> throw new SamskrtamException("UNSUPPORTED_QUIZ_TYPE", "Quiz type " + quizType + " is not a specific declension type.");
        };
    }
}
