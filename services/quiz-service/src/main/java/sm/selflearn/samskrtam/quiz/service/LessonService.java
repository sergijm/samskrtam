package sm.selflearn.samskrtam.quiz.service;

import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Mono;
import sm.selflearn.samskrtam.content.dto.LessonType;
import sm.selflearn.samskrtam.quiz.dto.VocabularyLesson;
import sm.selflearn.samskrtam.quiz.dto.GrammarLesson;
import sm.selflearn.samskrtam.quiz.dto.WordAnswerHistory;
import sm.selflearn.samskrtam.quiz.dto.QuestionAnswerHistory;

import java.util.Locale;
import java.util.UUID;

public interface LessonService {
    Mono<VocabularyLesson> getVocabularyLesson(String slug, UUID userId);
    Mono<GrammarLesson> getGrammarLesson(LessonType type, UUID userId);
    Mono<WordAnswerHistory> getWordAnswerHistory(String slug, UUID wordId, UUID userId, Pageable pageable, Locale locale);
    Mono<QuestionAnswerHistory> getQuestionAnswerHistory(String type, UUID questionId, UUID userId, Pageable pageable, Locale locale);
}