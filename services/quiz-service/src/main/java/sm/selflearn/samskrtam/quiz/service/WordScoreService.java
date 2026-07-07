package sm.selflearn.samskrtam.quiz.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import sm.selflearn.samskrtam.quiz.model.ItemType;
import sm.selflearn.samskrtam.quiz.model.QuizItemScore;
import sm.selflearn.samskrtam.quiz.repository.QuizItemScoreRepository;

import java.time.Instant;
import java.util.UUID;

/**
 * Обёртка над QuizItemScoreRepository для vocabulary-слов.
 * Заменяет старый WordScoreService, работавший через таблицу word_score.
 *
 * <p>Использует единую таблицу quiz.quiz_item_score (itemType = VOCABULARY_WORD).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WordScoreService {

    private final QuizItemScoreRepository quizItemScoreRepository;

    /**
     * Upsert vocabulary word score using QuizItemScore upsert.
     */
    public Mono<Void> upsertScore(UUID userId, UUID wordId, UUID lessonId, boolean isCorrect) {
        return quizItemScoreRepository.findByUserIdAndItemTypeAndExternalRefId(
                        userId, ItemType.VOCABULARY_WORD, wordId)
                .flatMap(existing -> {
                    int newScore = calculateScore(existing.getScore(), isCorrect);
                    Instant now = Instant.now();
                    int newStability = isCorrect
                            ? Math.min(10, existing.getStability() + 1)
                            : Math.max(1, existing.getStability() - 2);
                    int newConsecutiveMistakes = isCorrect ? 0 : existing.getConsecutiveMistakes() + 1;
                    Instant lastMistakeAt = isCorrect ? existing.getLastMistakeAt() : now;

                    return quizItemScoreRepository.upsertScore(
                            existing.getId(), userId, ItemType.VOCABULARY_WORD.name(), wordId,
                            newScore, newStability,
                            now, lastMistakeAt, newConsecutiveMistakes,
                            existing.getNextReviewAt());
                })
                .switchIfEmpty(Mono.defer(() -> {
                    int initialScore = calculateScore(0, isCorrect);
                    Instant now = Instant.now();

                    return quizItemScoreRepository.upsertScore(
                            UUID.randomUUID(), userId, ItemType.VOCABULARY_WORD.name(), wordId,
                            initialScore, 1,
                            now, isCorrect ? null : now,
                            isCorrect ? 0 : 1,
                            now.plusSeconds(86400L));
                }));
    }

    /**
     * Calculates the new score according to the quiz-generator-spec §2.5 formula.
     */
    public static int calculateScore(int currentScore, boolean isCorrect) {
        double raw;
        if (isCorrect) {
            raw = currentScore + (100.0 - currentScore) * 0.5;
        } else {
            double penalty = Math.max(0.15, 0.75 / Math.max(1, currentScore / 10));
            raw = currentScore - (currentScore - 5) * penalty;
        }
        // round5 with special rule: < 50 floor, >= 50 ceil
        int rounded = raw < 50
                ? (int) Math.floor(raw / 5.0) * 5
                : (int) Math.ceil(raw / 5.0) * 5;
        return Math.max(0, Math.min(100, rounded));
    }
}
