package sm.selflearn.samskrtam.content.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import sm.selflearn.samskrtam.common.SamskrtamException;
import sm.selflearn.samskrtam.content.dto.GeneratedQuizData;
import sm.selflearn.samskrtam.content.dto.GeneratedQuizQuestionDto;
import sm.selflearn.samskrtam.content.dto.LessonType;
import sm.selflearn.samskrtam.content.dto.VocabularyWordDto;
import sm.selflearn.samskrtam.content.model.*;
import sm.selflearn.samskrtam.content.repository.LessonRepository;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class GenerateQuizService {

    private final LessonRepository lessonRepository;
    private final QuestionGenerationService questionGenerationService;
    private final VocabularyService vocabularyService;
    private final GrammarContentService grammarContentService;

        public GeneratedQuizData generateQuizData(UUID quizId, Locale locale,
                                              String filterScope, String filterCaseTypes,
                                              String filterNumberTypes, String filterCombinations,
                                              String filterVowelTypes, String filterGenders) {

        Lesson lesson = lessonRepository.findById(quizId)
                .orElseThrow(() -> new SamskrtamException("LESSON_NOT_FOUND", "Lesson not found with ID: " + quizId));

        List<VocabularyWordDto> vocabularyWords = Collections.emptyList();
        if (LessonType.isVocabulary(lesson.getLessonType())) {
            vocabularyWords = vocabularyService.getVocabularyWordsForQuiz(lesson.getSlug(), lesson.getQuestionsPerSession() * 4);
        }

        List<GeneratedQuizQuestionDto> questions = questionGenerationService.generateQuestions(
                lesson, locale.getLanguage(), filterScope, filterCaseTypes, filterNumberTypes, filterCombinations,
                filterVowelTypes, filterGenders);
        List<GeneratedQuizQuestionDto> sortedQuestions = questions.stream()
                .sorted(Comparator.comparingInt(GeneratedQuizQuestionDto::getQuestionNumber))
                .toList();

        return GeneratedQuizData.builder()
                .lessonId(lesson.getId())
                .lessonType(lesson.getLessonType())
                .questionsPerSession(lesson.getQuestionsPerSession())
                .generatedQuestions(sortedQuestions)
                .vocabularyWords(vocabularyWords)
                .build();
    }
}

