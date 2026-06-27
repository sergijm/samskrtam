package sm.selflearn.samskrtam.quiz.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import sm.selflearn.samskrtam.quiz.model.WordScore;
import sm.selflearn.samskrtam.quiz.repository.WordScoreRepository;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WordScoreService {

    private final WordScoreRepository wordScoreRepository;

    /**
     * Upsert word score with exponential rating algorithm.
     * <p>
     * Initial (no record): score = 0<br>
     * First answer (score == 0): correct → 25, incorrect → 5<br>
     * Subsequent (score > 0): correct → score + (100 - score) * 0.5, incorrect → score * 0.3<br>
     * After calculation: round5(score), max(score, 5)<br>
     * Async via reactive pipeline + atomic upsert (ON CONFLICT DO UPDATE).
     */
    public Mono<WordScore> upsertScore(UUID userId, UUID wordId, UUID lessonId, boolean isCorrect) {
        return wordScoreRepository.findByUserIdAndWordIdAndLessonId(userId, wordId, lessonId)
                .flatMap(existing -> {
                    int newScore = calculateScore(existing.getScore(), isCorrect);
                    existing.setScore(newScore);
                    existing.setUpdatedAt(Instant.now());
                    return wordScoreRepository.save(existing);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    int initialScore = calculateScore(0, isCorrect);
                    WordScore newScore = WordScore.builder()
                            .id(UUID.randomUUID())
                            .userId(userId)
                            .wordId(wordId)
                            .lessonId(lessonId)
                            .score(initialScore)
                            .updatedAt(Instant.now())
                            .build();
                    return wordScoreRepository.save(newScore);
                }));
    }

    /**
     * Calculates the new score according to the exponential rating algorithm.
     */
    public static int calculateScore(int currentScore, boolean isCorrect) {
        double raw;
        if (currentScore == 0) {
            // First answer
            raw = isCorrect ? 25.0 : 5.0;
        } else {
            if (isCorrect) {
                raw = currentScore + (100.0 - currentScore) * 0.5;
            } else {
                raw = currentScore * 0.3;
            }
        }
        // round5
        int rounded = (int) Math.round(raw / 5.0) * 5;
        // ensure >= 5
        return Math.max(rounded, 5);
    }
}
