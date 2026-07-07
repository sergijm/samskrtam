package sm.selflearn.samskrtam.quiz.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import sm.selflearn.samskrtam.quiz.model.ItemType;
import sm.selflearn.samskrtam.quiz.repository.QuizItemScoreRepository;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

/**
 * Сервис для записи score по грамматическим формам в quiz_item_score.
 * Заменяет старый GrammarFormScoreService, работавший через таблицу grammar_form_score.
 *
 * <p>Использует единую таблицу quiz.quiz_item_score (itemType = DECLENSION_FORM).
 * externalRefId вычисляется детерминированно из (gender, caseType, numberType).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GrammarFormScoreService {

    private final QuizItemScoreRepository quizItemScoreRepository;

    /**
     * Initial score formula (for backward compat with in-session scoring).
     */
    public static int calculateScore(int currentScore, boolean isCorrect) {
        double raw;
        if (isCorrect) {
            raw = currentScore + (100.0 - currentScore) * 0.5;
        } else {
            double penalty = Math.max(0.15, 0.75 / Math.max(1, currentScore / 10));
            raw = currentScore - (currentScore - 5) * penalty;
        }
        int rounded = raw < 50
                ? (int) Math.floor(raw / 5.0) * 5
                : (int) Math.ceil(raw / 5.0) * 5;
        return Math.max(0, Math.min(100, rounded));
    }

    /**
     * Upsert score for a grammar form in quiz_item_score.
     * externalRefId = deterministic UUID from (gender, caseType, numberType).
     */
    public Mono<Void> upsertScore(
            UUID userId, UUID lessonId,
            String gender, String caseType, String numberType,
            boolean isCorrect) {

        UUID externalRefId = deterministicExternalRefId(gender, caseType, numberType);

        return quizItemScoreRepository.findByUserIdAndItemTypeAndExternalRefId(
                        userId, ItemType.DECLENSION_FORM, externalRefId)
                .flatMap(existing -> {
                    int newScore = calculateScore(existing.getScore(), isCorrect);
                    Instant now = Instant.now();
                    int newStability = isCorrect
                            ? Math.min(10, existing.getStability() + 1)
                            : Math.max(1, existing.getStability() - 2);
                    int newConsecutiveMistakes = isCorrect ? 0 : existing.getConsecutiveMistakes() + 1;
                    Instant lastMistakeAt = isCorrect ? existing.getLastMistakeAt() : now;

                    return quizItemScoreRepository.upsertScore(
                            existing.getId(), userId, ItemType.DECLENSION_FORM.name(), externalRefId,
                            newScore, newStability,
                            now, lastMistakeAt, newConsecutiveMistakes,
                            existing.getNextReviewAt());
                })
                .switchIfEmpty(Mono.defer(() -> {
                    int initialScore = calculateScore(0, isCorrect);
                    Instant now = Instant.now();

                    return quizItemScoreRepository.upsertScore(
                            UUID.randomUUID(), userId, ItemType.DECLENSION_FORM.name(), externalRefId,
                            initialScore, 1,
                            now, isCorrect ? null : now,
                            isCorrect ? 0 : 1,
                            now.plusSeconds(86400L));
                }));
    }

    /**
     * Детерминированный UUID из (gender, caseType, numberType) — соответствует
     * {@link GrammarQuestionProgressFactory#deterministicId}.
     */
    private UUID deterministicExternalRefId(String gender, String caseType, String numberType) {
        return UUID.nameUUIDFromBytes(
                (gender + ":" + caseType + ":" + numberType).getBytes(StandardCharsets.UTF_8));
    }
}
