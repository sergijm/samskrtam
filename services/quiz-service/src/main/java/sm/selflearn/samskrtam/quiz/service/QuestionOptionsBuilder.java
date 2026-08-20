package sm.selflearn.samskrtam.quiz.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import sm.selflearn.samskrtam.content.dto.GeneratedQuizQuestionDto;
import sm.selflearn.samskrtam.content.dto.LessonType;
import sm.selflearn.samskrtam.content.dto.VocabularyWordDto;
import sm.selflearn.samskrtam.quiz.dto.QuestionDto;
import sm.selflearn.samskrtam.quiz.model.QuizSession;

import java.util.List;

/**
 * Построение {@link QuestionDto} с опциями для каждого вопроса сессии.
 * Выделен из {@link QuizDataAssembler} для компактности.
 *
 * <p>Диспетчеризует по типу вопроса:
 * <ul>
 *   <li>FORM_BY_CASE (default) — выбор правильной формы по падежу/числу</li>
 *   <li>CASE_BY_FORM — выбор падежа/числа по форме (single-select)</li>
 *   <li>ENDING_MATCH — выбор падежа/числа по окончанию (multi-select)</li>
 *   <li>Vocabulary — выбор перевода слова</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class QuestionOptionsBuilder {

    private final LexicalOptionGeneratorService lexicalOptionGeneratorService;
    private final DeclensionOptionGeneratorService declensionOptionGeneratorService;

    /**
     * Построить {@link QuestionDto} с опциями для одного вопроса.
     */
    public Mono<QuestionDto> buildOptions(
            QuizSession session,
            GeneratedQuizQuestionDto generatedQuestion,
            List<VocabularyWordDto> allVocabularyWords,
            String userLocale) {

        // --- Vocabulary branch ---
        if (LessonType.isVocabulary(session.getLessonType())) {
            return buildVocabularyQuestion(generatedQuestion, allVocabularyWords, userLocale);
        }

        // --- Declension branch — dispatch by questionType ---
        return buildDeclensionQuestion(generatedQuestion);
    }

    // ================== Private builders ==================

    private Mono<QuestionDto> buildVocabularyQuestion(
            GeneratedQuizQuestionDto gq,
            List<VocabularyWordDto> allVocabularyWords,
            String userLocale) {

        VocabularyWordDto correctWord = allVocabularyWords.stream()
                .filter(w -> w.getId().equals(gq.getVocabularyWordId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException(
                        "Vocabulary word not found for ID: " + gq.getVocabularyWordId()));

        return lexicalOptionGeneratorService.generateOptions(
                        correctWord, allVocabularyWords,
                        gq.getQuestionSourceLanguage(), gq.getQuestionTargetLanguage(), userLocale)
                .map(options -> QuestionDto.builder()
                        .id(gq.getId())
                        .questionNumber(gq.getQuestionNumber())
                        .text(gq.getText())
                        .options(options)
                        .stem(gq.getStem())
                        .caseType(gq.getCaseType())
                        .numberType(gq.getNumberType())
                        .stemDevanagari(gq.getStemDevanagari())
                        .stemTranslationRu(gq.getStemTranslationRu())
                        .stemTranslationEn(gq.getStemTranslationEn())
                        .gender(gq.getGender())
                        .build());
    }

    private Mono<QuestionDto> buildDeclensionQuestion(GeneratedQuizQuestionDto gq) {
        String qt = gq.getQuestionType();

        // FORM_BY_CASE (default, null)
        if (qt == null || "FORM_BY_CASE".equals(qt)) {
            return buildFormByCaseQuestion(gq);
        }

        // CASE_BY_FORM: prompt = formIast/formDevanagari, single-select
        if ("CASE_BY_FORM".equals(qt)) {
            return buildCaseByFormQuestion(gq);
        }

        // ENDING_MATCH: prompt = ending, multi-select
        if ("ENDING_MATCH".equals(qt)) {
            return buildEndingMatchQuestion(gq);
        }

        // Fallback: unknown questionType → treat as FORM_BY_CASE
        return buildFormByCaseQuestion(gq);
    }

    private Mono<QuestionDto> buildFormByCaseQuestion(GeneratedQuizQuestionDto gq) {
        return declensionOptionGeneratorService.generateOptions(
                        gq.getDeclensionStemId(),
                        gq.getTargetCase(),
                        gq.getTargetNumber(),
                        gq.getCorrectFormIast())
                .map(options -> QuestionDto.builder()
                        .id(gq.getId())
                        .questionNumber(gq.getQuestionNumber())
                        .text(gq.getText())
                        .options(options)
                        .stem(gq.getStem())
                        .caseType(gq.getCaseType())
                        .numberType(gq.getNumberType())
                        .stemDevanagari(gq.getStemDevanagari())
                        .stemTranslationRu(gq.getStemTranslationRu())
                        .stemTranslationEn(gq.getStemTranslationEn())
                        .gender(gq.getGender())
                        .build());
    }

    private Mono<QuestionDto> buildCaseByFormQuestion(GeneratedQuizQuestionDto gq) {
        String gender = gq.getGender() != null ? gq.getGender() : "UNSPECIFIED";
        return declensionOptionGeneratorService.generateCaseOptions(
                        gq.getDeclensionStemId(),
                        gq.getTargetCase(),
                        gq.getTargetNumber(),
                        gender)
                .map(options -> QuestionDto.builder()
                        .id(gq.getId())
                        .questionNumber(gq.getQuestionNumber())
                        .text(gq.getText())
                        .questionType("CASE_BY_FORM")
                        .multiSelect(false)
                        .formIast(gq.getCorrectFormIast())
                        .formDevanagari(gq.getCorrectFormDevanagari())
                        .caseEnding(gq.getCaseEnding())
                        .options(options)
                        .stem(gq.getStem())
                        .stemDevanagari(gq.getStemDevanagari())
                        .stemTranslationRu(gq.getStemTranslationRu())
                        .stemTranslationEn(gq.getStemTranslationEn())
                        .gender(gq.getGender())
                        .build());
    }

    private Mono<QuestionDto> buildEndingMatchQuestion(GeneratedQuizQuestionDto gq) {
        String gender = gq.getGender() != null ? gq.getGender() : "UNSPECIFIED";
        return declensionOptionGeneratorService.generateCaseOptions(
                        gq.getDeclensionStemId(),
                        gq.getTargetCase(),
                        gq.getTargetNumber(),
                        gender)
                .map(options -> QuestionDto.builder()
                        .id(gq.getId())
                        .questionNumber(gq.getQuestionNumber())
                        .text(gq.getText())
                        .questionType("ENDING_MATCH")
                        .multiSelect(true)
                        .formIast(gq.getCorrectFormIast())
                        .formDevanagari(gq.getCorrectFormDevanagari())
                        .caseEnding(gq.getCaseEnding())
                        .options(options)
                        .stem(gq.getStem())
                        .stemDevanagari(gq.getStemDevanagari())
                        .stemTranslationRu(gq.getStemTranslationRu())
                        .stemTranslationEn(gq.getStemTranslationEn())
                        .gender(gq.getGender())
                        .build());
    }
}
