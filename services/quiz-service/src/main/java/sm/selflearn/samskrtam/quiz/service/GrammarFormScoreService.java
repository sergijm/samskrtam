package sm.selflearn.samskrtam.quiz.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import sm.selflearn.samskrtam.quiz.model.GrammarFormScore;
import sm.selflearn.samskrtam.quiz.repository.GrammarFormScoreRepository;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class GrammarFormScoreService {

    private final GrammarFormScoreRepository repository;

    /**
     * Linear score model for declensions/conjugations:
     *   correct:   min(100, score + 10)
     *   incorrect: max(0,   score - 50)
     */
    public static int calculateScore(int currentScore, boolean isCorrect) {
        return isCorrect
                ? Math.min(100, currentScore + 10)
                : Math.max(0, currentScore - 50);
    }

    public Mono<GrammarFormScore> upsertScore(
            UUID userId, UUID lessonId,
            String caseType, String numberType,
            boolean isCorrect) {

        return repository.findByUserIdAndLessonIdAndCaseTypeAndNumberType(
                        userId, lessonId, caseType, numberType)
                .flatMap(existing -> {
                    int newScore = calculateScore(existing.getScore(), isCorrect);
                    existing.setScore(newScore);
                    existing.setUpdatedAt(Instant.now());
                    return repository.save(existing);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    GrammarFormScore entry = GrammarFormScore.builder()
                            .id(UUID.randomUUID())
                            .userId(userId)
                            .lessonId(lessonId)
                            .caseType(caseType)
                            .numberType(numberType)
                            .score(calculateScore(0, isCorrect))
                            .updatedAt(Instant.now())
                            .build();
                    return repository.save(entry);
                }));
    }
}
