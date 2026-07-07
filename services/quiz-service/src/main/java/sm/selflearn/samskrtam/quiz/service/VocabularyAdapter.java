package sm.selflearn.samskrtam.quiz.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import sm.selflearn.samskrtam.content.dto.VocabularyWordDto;
import sm.selflearn.samskrtam.quiz.model.ItemType;
import sm.selflearn.samskrtam.quiz.model.QuizItem;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Адаптер между ContentClient и QuizGenerator для itemType = VOCABULARY_WORD.
 *
 * <p>Отвечает за:
 * <ul>
 *   <li>Получение плоского списка word_id из content-service по slug урока/категории</li>
 *   <li>Проверку существования word_id через ContentClient (не FK — эвентуальная целостность)</li>
 *   <li>Передачу externalRefIds в QuizGenerator.generate()</li>
 * </ul>
 *
 * <p>Проверка существования выполняется на уровне приложения:
 * если word_id не существует в content-service, ContentClient вернёт ошибку
 * (SamskrtamException VOCABULARY_WORD_NOT_FOUND), которая будет обработана
 * глобальным exception handler'ом.
 *
 * @see ContentClient#getVocabularyWordIdsForLesson(String)
 * @see ContentClient#getVocabularyWordById(UUID)
 * @see QuizGenerator#generate(UUID, ItemType, List)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class VocabularyAdapter {

    private final ContentClient contentClient;
    private final QuizGenerator quizGenerator;

    /**
     * Получить список QuizItem для сессии лексики по slug урока.
     * Шаги:
     * <ol>
     *   <li>Запросить плоский список word_id из content-service</li>
     *   <li>Проверить, что scope не пустой</li>
     *   <li>Вызвать QuizGenerator.generate() с ItemType.VOCABULARY_WORD</li>
     * </ol>
     *
     * @param userId идентификатор пользователя
     * @param slug   slug урока/категории лексики
     * @return список QuizItem для сессии
     */
    public Mono<List<QuizItem>> generateVocabularyQuiz(UUID userId, String slug) {
        return contentClient.getVocabularyWordIdsForLesson(slug)
                .flatMap(wordIds -> {
                    if (wordIds == null || wordIds.isEmpty()) {
                        log.warn("No vocabulary words found for lesson slug: {}", slug);
                        return Mono.just(Collections.<QuizItem>emptyList());
                    }
                    List<UUID> refIds = wordIds.stream().collect(Collectors.toList());
                    return quizGenerator.generate(userId, ItemType.VOCABULARY_WORD, refIds);
                });
    }

    /**
     * Проверить существование vocabulary word по ID.
     * Выбрасывает SamskrtamException если слово не найдено.
     * Используется для валидации externalRefId перед сохранением answer.
     */
    public Mono<VocabularyWordDto> validateWordExists(UUID wordId) {
        return contentClient.getVocabularyWordById(wordId);
    }
}