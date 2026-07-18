package sm.selflearn.samskrtam.quiz.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import sm.selflearn.samskrtam.quiz.dto.AnswerHistoryEntry;
import sm.selflearn.samskrtam.quiz.dto.WordAnswerHistory;
import sm.selflearn.samskrtam.quiz.mapper.QuizAnswerMapper;
import sm.selflearn.samskrtam.quiz.model.QuizAnswer;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Строит {@link WordAnswerHistory} — историю ответов по конкретному слову.
 */
@Component
@RequiredArgsConstructor
public class WordAnswerHistoryBuilder {

    private final UserSessionService userSessionService;
    private final QuizAnswerMapper quizAnswerMapper;

    /**
     * Создаёт {@link WordAnswerHistory} для конкретного слова.
     *
     * @param wordId   id слова
     * @param lessonId id урока
     * @param wordIast слово в IAST
     * @param userId   id пользователя
     * @param pageable пагинация
     * @return Mono с WordAnswerHistory
     */
    public Mono<WordAnswerHistory> build(
            UUID wordId, UUID lessonId, String wordIast,
            UUID userId, Pageable pageable) {

        Mono<List<QuizAnswer>> answersMono =
                userSessionService.getWordAnswers(userId, wordId, lessonId);
        Mono<Long> totalMono =
                userSessionService.countWordAnswers(userId, wordId, lessonId);

        return Mono.zip(answersMono, totalMono)
                .map(tuple -> {
                    List<QuizAnswer> answers = tuple.getT1();
                    long total = tuple.getT2();

                    List<AnswerHistoryEntry> entries = answers.stream()
                            .map(quizAnswerMapper::toAnswerHistoryEntry)
                            .collect(Collectors.toList());

                    return WordAnswerHistory.builder()
                            .wordId(wordId)
                            .lessonId(lessonId)
                            .word(wordIast)
                            .entries(entries)
                            .page(pageable.getPageNumber())
                            .size(pageable.getPageSize())
                            .total((int) total)
                            .build();
                });
    }
}
