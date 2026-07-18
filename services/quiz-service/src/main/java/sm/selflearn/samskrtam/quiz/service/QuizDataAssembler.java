package sm.selflearn.samskrtam.quiz.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import sm.selflearn.samskrtam.content.dto.LessonType;
import sm.selflearn.samskrtam.content.dto.QuestionLanguage;
import sm.selflearn.samskrtam.content.dto.VocabularyWordDto;
import sm.selflearn.samskrtam.quiz.dto.AnswerRequest;
import sm.selflearn.samskrtam.content.dto.GeneratedQuizQuestionDto;
import sm.selflearn.samskrtam.quiz.dto.QuestionDto;
import sm.selflearn.samskrtam.quiz.dto.StartOrResumeResponse;
import sm.selflearn.samskrtam.quiz.mapper.QuizSessionMapper;
import sm.selflearn.samskrtam.quiz.model.QuizAnswer;
import sm.selflearn.samskrtam.quiz.model.QuizSession;
import sm.selflearn.samskrtam.quiz.repository.QuizAnswerRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuizDataAssembler {

    private final ContentClient contentClient;
    private final QuizAnswerRepository quizAnswerRepository;
    private final QuizSessionMapper quizSessionMapper;
    private final DeclensionOptionGeneratorService declensionOptionGeneratorService;
    private final LexicalOptionGeneratorService lexicalOptionGeneratorService;

    public Mono<StartOrResumeResponse> assembleResponse(QuizSession session, List<GeneratedQuizQuestionDto> generatedQuestions, List<VocabularyWordDto> allVocabularyWords, String userLocale) {
        return contentClient.getLessonItem(session.getLessonId())
                .flatMap(quizSummary -> quizAnswerRepository.findBySessionId(session.getId())
                        .collectList()
                        .flatMap(answeredQuestions -> {
                            Set<UUID> answeredQuestionIds = answeredQuestions.stream()
                                    .map(QuizAnswer::getQuestionId)
                                    .collect(Collectors.toSet());

                            List<GeneratedQuizQuestionDto> sortedQuestions = generatedQuestions.stream()
                                    .sorted(Comparator.comparing(GeneratedQuizQuestionDto::getQuestionNumber))
                                    .collect(Collectors.toList());

                            return Flux.fromIterable(sortedQuestions)
                                    .flatMap(generatedQuestion -> generateQuestionOptions(session, generatedQuestion, allVocabularyWords, userLocale))
                                    .collectList()
                                    .map(questions -> {
                                        questions.sort(Comparator.comparing(QuestionDto::getQuestionNumber));
                                        return quizSessionMapper.toStartOrResumeResponse(session, questions, quizSummary, List.copyOf(answeredQuestionIds));
                                    });
                        }));
    }

    public String determineSelectedOptionIast(AnswerRequest request, GeneratedQuizQuestionDto generatedQuestion, List<VocabularyWordDto> allVocabularyWords) {
        if (generatedQuestion.getVocabularyWordId() != null) {
            VocabularyWordDto selectedWord = allVocabularyWords.stream()
                    .filter(w -> w.getId().equals(request.getSelectedOptionId()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Selected vocabulary word not found: " + request.getSelectedOptionId()));

            if (generatedQuestion.getQuestionTargetLanguage() == QuestionLanguage.SANSKRIT) {
                return selectedWord.getWordIast();
            } else if (generatedQuestion.getQuestionTargetLanguage() == QuestionLanguage.RUSSIAN) {
                return selectedWord.getTranslationRu();
            } else if (generatedQuestion.getQuestionTargetLanguage() == QuestionLanguage.ENGLISH) {
                return selectedWord.getTranslationEn();
            } else {
                return null;
            }
        } else {
            return request.getSelectedFormIast();
        }
    }

    public UUID findCorrectWordId(GeneratedQuizQuestionDto generatedQuestion, List<VocabularyWordDto> allVocabularyWords) {
        if (generatedQuestion.getVocabularyWordId() != null) {
            return allVocabularyWords.stream()
                .filter(w -> w.getId().equals(generatedQuestion.getVocabularyWordId()))
                .map(VocabularyWordDto::getId)
                .findFirst()
                .orElse(null);
        }
        return null;
    }

    public String findCorrectAnswerText(GeneratedQuizQuestionDto generatedQuestion, List<VocabularyWordDto> allVocabularyWords, String userLocale) {
        if (generatedQuestion.getVocabularyWordId() != null) {
            VocabularyWordDto correctWord = allVocabularyWords.stream()
                .filter(w -> w.getId().equals(generatedQuestion.getVocabularyWordId()))
                .findFirst()
                .orElse(null);

            if (correctWord != null) {
                if (generatedQuestion.getQuestionTargetLanguage() == QuestionLanguage.SANSKRIT) {
                    return correctWord.getWordIast();
                } else if (generatedQuestion.getQuestionTargetLanguage() == QuestionLanguage.RUSSIAN) {
                    return correctWord.getTranslationRu();
                } else if (generatedQuestion.getQuestionTargetLanguage() == QuestionLanguage.ENGLISH) {
                    return correctWord.getTranslationEn();
                }
            }
        }
        return generatedQuestion.getCorrectFormIast();
    }

        private Mono<QuestionDto> generateQuestionOptions(QuizSession session, GeneratedQuizQuestionDto generatedQuestion, List<VocabularyWordDto> allVocabularyWords, String userLocale) {
        // --- Vocabulary branch ---
        if (LessonType.isVocabulary(session.getLessonType())) {
            VocabularyWordDto correctWord = allVocabularyWords.stream()
                    .filter(w -> w.getId().equals(generatedQuestion.getVocabularyWordId()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Vocabulary word not found for ID: " + generatedQuestion.getVocabularyWordId()));

            return lexicalOptionGeneratorService.generateOptions(
                    correctWord,
                    allVocabularyWords,
                    generatedQuestion.getQuestionSourceLanguage(),
                    generatedQuestion.getQuestionTargetLanguage(),
                    userLocale
            ).map(options -> QuestionDto.builder()
                    .id(generatedQuestion.getId())
                    .questionNumber(generatedQuestion.getQuestionNumber())
                    .text(generatedQuestion.getText())
                    .options(options)
                    .stem(generatedQuestion.getStem())
                    .caseType(generatedQuestion.getCaseType())
                    .numberType(generatedQuestion.getNumberType())
                    .stemDevanagari(generatedQuestion.getStemDevanagari())
                    .stemTranslationRu(generatedQuestion.getStemTranslationRu())
                    .stemTranslationEn(generatedQuestion.getStemTranslationEn())
                    .gender(generatedQuestion.getGender())
                    .build());
        }

        // --- Declension branch — dispatch by questionType ---
        String qt = generatedQuestion.getQuestionType();

        // FORM_BY_CASE (default, null)
        if (qt == null || "FORM_BY_CASE".equals(qt)) {
            return declensionOptionGeneratorService.generateOptions(
                    generatedQuestion.getDeclensionStemId(),
                    generatedQuestion.getTargetCase(),
                    generatedQuestion.getTargetNumber(),
                    generatedQuestion.getCorrectFormIast()
            ).map(options -> QuestionDto.builder()
                    .id(generatedQuestion.getId())
                    .questionNumber(generatedQuestion.getQuestionNumber())
                    .text(generatedQuestion.getText())
                    .options(options)
                    .stem(generatedQuestion.getStem())
                    .caseType(generatedQuestion.getCaseType())
                    .numberType(generatedQuestion.getNumberType())
                    .stemDevanagari(generatedQuestion.getStemDevanagari())
                    .stemTranslationRu(generatedQuestion.getStemTranslationRu())
                    .stemTranslationEn(generatedQuestion.getStemTranslationEn())
                    .gender(generatedQuestion.getGender())
                    .build());
        }

        // CASE_BY_FORM: prompt = formIast/formDevanagari, single-select
        if ("CASE_BY_FORM".equals(qt)) {
            String gender = generatedQuestion.getGender() != null ? generatedQuestion.getGender() : "UNSPECIFIED";
            return declensionOptionGeneratorService.generateCaseOptions(
                    generatedQuestion.getDeclensionStemId(),
                    generatedQuestion.getTargetCase(),
                    generatedQuestion.getTargetNumber(),
                    gender
            ).map(options -> QuestionDto.builder()
                    .id(generatedQuestion.getId())
                    .questionNumber(generatedQuestion.getQuestionNumber())
                    .text(generatedQuestion.getText())
                    .questionType("CASE_BY_FORM")
                    .multiSelect(false)
                    .formIast(generatedQuestion.getCorrectFormIast())
                    .formDevanagari(generatedQuestion.getCorrectFormDevanagari())
                    .caseEnding(generatedQuestion.getCaseEnding())
                    .options(options)
                    .stem(generatedQuestion.getStem())
                    // NOT setting caseType/numberType — prompt is the form, not the answer hint
                    .stemDevanagari(generatedQuestion.getStemDevanagari())
                    .stemTranslationRu(generatedQuestion.getStemTranslationRu())
                    .stemTranslationEn(generatedQuestion.getStemTranslationEn())
                    .gender(generatedQuestion.getGender())
                    .build());
        }

        // ENDING_MATCH: prompt = ending, multi-select
        if ("ENDING_MATCH".equals(qt)) {
            String gender = generatedQuestion.getGender() != null ? generatedQuestion.getGender() : "UNSPECIFIED";
            return declensionOptionGeneratorService.generateCaseOptions(
                    generatedQuestion.getDeclensionStemId(),
                    generatedQuestion.getTargetCase(),
                    generatedQuestion.getTargetNumber(),
                    gender
            ).map(options -> QuestionDto.builder()
                    .id(generatedQuestion.getId())
                    .questionNumber(generatedQuestion.getQuestionNumber())
                    .text(generatedQuestion.getText())
                    .questionType("ENDING_MATCH")
                    .multiSelect(true)
                    .formIast(generatedQuestion.getCorrectFormIast())
                    .formDevanagari(generatedQuestion.getCorrectFormDevanagari())
                    .caseEnding(generatedQuestion.getCaseEnding())
                    .options(options)
                    .stem(generatedQuestion.getStem())
                    // NOT setting caseType/numberType — prompt is the ending, not the answer hint
                    .stemDevanagari(generatedQuestion.getStemDevanagari())
                    .stemTranslationRu(generatedQuestion.getStemTranslationRu())
                    .stemTranslationEn(generatedQuestion.getStemTranslationEn())
                    .gender(generatedQuestion.getGender())
                    .build());
        }

        // Fallback: unknown questionType → treat as FORM_BY_CASE
        return declensionOptionGeneratorService.generateOptions(
                generatedQuestion.getDeclensionStemId(),
                generatedQuestion.getTargetCase(),
                generatedQuestion.getTargetNumber(),
                generatedQuestion.getCorrectFormIast()
        ).map(options -> QuestionDto.builder()
                .id(generatedQuestion.getId())
                .questionNumber(generatedQuestion.getQuestionNumber())
                .text(generatedQuestion.getText())
                .options(options)
                .stem(generatedQuestion.getStem())
                .caseType(generatedQuestion.getCaseType())
                .numberType(generatedQuestion.getNumberType())
                .stemDevanagari(generatedQuestion.getStemDevanagari())
                .stemTranslationRu(generatedQuestion.getStemTranslationRu())
                .stemTranslationEn(generatedQuestion.getStemTranslationEn())
                .gender(generatedQuestion.getGender())
                                .build());
    }
}
