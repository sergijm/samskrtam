package sm.selflearn.samskrtam.quiz.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import sm.selflearn.samskrtam.quiz.constants.ProgressConstants;
import sm.selflearn.samskrtam.quiz.dto.QuizProgressByCaseDto;
import sm.selflearn.samskrtam.quiz.mapper.QuizProgressMapper;
import sm.selflearn.samskrtam.quiz.model.ItemType;
import sm.selflearn.samskrtam.quiz.repository.QuizItemScoreRepository;

import java.util.UUID;

/**
 * Сервис для агрегации прогресса по падежам (caseType) с вычислением
 * доли успешно пройденных комбинаций (gender,numberType).
 *
 * <p>Использует единую таблицу quiz.quiz_item_score (itemType = DECLENSION_FORM).
 * Примечание: агрегация по уроку (lessonId) невозможна напрямую через quiz_item_score,
 * так как таблица не содержит lesson_id. В текущей реализации возвращается пустой Flux,
 * так как quiz_item_score не привязан к уроку. Функциональность будет восстановлена
 * после согласования scope-запроса с content-service (см. quiz-generator-spec §2.3).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QuizProgressAggregationService {

    private final QuizItemScoreRepository quizItemScoreRepository;
    private final QuizProgressMapper quizProgressMapper;

    /**
     * Агрегирует прогресс по caseType.
     *
     * @param userId   пользователь
     * @param lessonId урок (не используется, см. JavaDoc класса)
     * @return Flux с агрегированным прогрессом по каждому caseType (пока пустой)
     */
    public Flux<QuizProgressByCaseDto> aggregateProgressByCase(UUID userId, UUID lessonId) {
        // TODO: после согласования scope-запроса с content-service
        // восстановить агрегацию через findByUserIdAndItemType
        // с последующей группировкой по caseType из externalRefId
        return Flux.empty();
    }
}
