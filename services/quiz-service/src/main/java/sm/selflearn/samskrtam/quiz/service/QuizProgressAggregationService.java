package sm.selflearn.samskrtam.quiz.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import sm.selflearn.samskrtam.quiz.constants.ProgressConstants;
import sm.selflearn.samskrtam.quiz.dto.GrammarQuestionProgress;
import sm.selflearn.samskrtam.quiz.dto.QuizProgressByCaseDto;
import sm.selflearn.samskrtam.quiz.dto.QuizProgressByNumberDto;
import sm.selflearn.samskrtam.quiz.dto.WordStatus;
import sm.selflearn.samskrtam.quiz.model.ItemType;
import sm.selflearn.samskrtam.quiz.repository.QuizItemScoreRepository;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Сервис для агрегации прогресса по падежам (caseType) и числам (numberType)
 * с вычислением доли успешно пройденных комбинаций.
 *
 * <p>Использует уже построенный {@link sm.selflearn.samskrtam.quiz.dto.GrammarLesson}
 * для агрегации, т.к. quiz_item_score не содержит lesson_id.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QuizProgressAggregationService {

    private final QuizItemScoreRepository quizItemScoreRepository;

    /**
     * Агрегирует прогресс по caseType из списка GrammarQuestionProgress.
     */
    public Flux<QuizProgressByCaseDto> aggregateProgressByCase(List<GrammarQuestionProgress> questions) {
        Map<String, List<GrammarQuestionProgress>> byCase = questions.stream()
                .collect(Collectors.groupingBy(GrammarQuestionProgress::getCaseType));

        return Flux.fromIterable(byCase.entrySet().stream()
                .map(entry -> {
                    String caseType = entry.getKey();
                    List<GrammarQuestionProgress> items = entry.getValue();
                    int total = items.size();
                    int learned = (int) items.stream()
                            .filter(q -> q.getStatus() == WordStatus.MASTERED
                                    || q.getStatus() == WordStatus.REVIEW)
                            .count();
                    int aggregatedProgress = total > 0
                            ? (int) Math.round(items.stream()
                                    .mapToInt(GrammarQuestionProgress::getScore)
                                    .average().orElse(0))
                            : 0;
                    return new QuizProgressByCaseDto(caseType, aggregatedProgress, total, learned);
                })
                .toList());
    }

    /**
     * Агрегирует прогресс по numberType из списка GrammarQuestionProgress.
     */
    public Flux<QuizProgressByNumberDto> aggregateProgressByNumber(List<GrammarQuestionProgress> questions) {
        Map<String, List<GrammarQuestionProgress>> byNumber = questions.stream()
                .collect(Collectors.groupingBy(GrammarQuestionProgress::getNumberType));

        return Flux.fromIterable(byNumber.entrySet().stream()
                .map(entry -> {
                    String numberType = entry.getKey();
                    List<GrammarQuestionProgress> items = entry.getValue();
                    int total = items.size();
                    int learned = (int) items.stream()
                            .filter(q -> q.getStatus() == WordStatus.MASTERED
                                    || q.getStatus() == WordStatus.REVIEW)
                            .count();
                    int aggregatedProgress = total > 0
                            ? (int) Math.round(items.stream()
                                    .mapToInt(GrammarQuestionProgress::getScore)
                                    .average().orElse(0))
                            : 0;
                    return new QuizProgressByNumberDto(numberType, aggregatedProgress, total, learned);
                })
                .toList());
    }

    /**
     * Агрегирует прогресс по caseType напрямую через quiz_item_score (без привязки к уроку).
     * Use {@link #aggregateProgressByCase(List)} for per-lesson aggregation.
     */
    @Deprecated
    public Flux<QuizProgressByCaseDto> aggregateProgressByCase(UUID userId, UUID lessonId) {
        return Flux.empty();
    }
}
