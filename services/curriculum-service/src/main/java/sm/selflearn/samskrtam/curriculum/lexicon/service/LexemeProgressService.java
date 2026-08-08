package sm.selflearn.samskrtam.curriculum.lexicon.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sm.selflearn.samskrtam.curriculum.lexicon.dto.LexemeProgressDto;
import sm.selflearn.samskrtam.curriculum.lexicon.model.Lexeme;
import sm.selflearn.samskrtam.curriculum.lexicon.model.UserLexemeProgress;
import sm.selflearn.samskrtam.curriculum.lexicon.model.UserLexemeProgressId;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.LexemeRepository;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.UserLexemeProgressRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Прогресс освоения лексем ({@code user_lexeme_progress}), task-curriculum-15 §8/§9.
 * Пересчёт masteryScore/nextReviewAt по формуле ADR-007 (quiz-generator-spec.md §2.5),
 * согласованной с quiz-service, чтобы «due review» ощущался пользователем единообразно.
 */
@Service
@RequiredArgsConstructor
public class LexemeProgressService {

    /** Порог MASTERED (тот же, что ProgressConstants.MASTERED_LOWER_THRESHOLD = 90). */
    public static final int MASTERED_THRESHOLD = 90;

    private final UserLexemeProgressRepository progressRepository;
    private final LexemeRepository lexemeRepository;

    @Transactional(readOnly = true)
    public List<LexemeProgressDto> getProgress(UUID userId, List<UUID> lexemeIds) {
        if (lexemeIds == null || lexemeIds.isEmpty()) {
            return List.of();
        }
        List<LexemeProgressDto> result = new ArrayList<>();
        for (UUID lexemeId : lexemeIds) {
            progressRepository.findById(id(userId, lexemeId))
                    .ifPresent(p -> result.add(toDto(p)));
        }
        return result;
    }

    @Transactional
    public LexemeProgressDto recordAnswer(UUID userId, UUID lexemeId, boolean correct) {
        UserLexemeProgress progress = progressRepository
                .findById(id(userId, lexemeId))
                .orElse(null);
        if (progress == null) {
            Lexeme lexeme = lexemeRepository.findById(lexemeId).orElse(null);
            progress = new UserLexemeProgress();
            progress.setId(id(userId, lexemeId));
            progress.setLexeme(lexeme);
            progress.setExposureCount(0);
            progress.setMasteryScore((short) 0);
            progress.setCorrectCount(0);
            progress.setIncorrectCount(0);
        }
        return applyAnswer(progress, correct);
    }

    private static UserLexemeProgressId id(UUID userId, UUID lexemeId) {
        UserLexemeProgressId id = new UserLexemeProgressId();
        id.setUserId(userId);
        id.setLexemeId(lexemeId);
        return id;
    }

    private LexemeProgressDto applyAnswer(UserLexemeProgress progress, boolean correct) {
        int prevScore = progress.getMasteryScore() == null ? 0 : progress.getMasteryScore();
        int exposure = progress.getExposureCount() == null ? 0 : progress.getExposureCount();

        short newScore;
        if (correct) {
            progress.setCorrectCount((progress.getCorrectCount() == null ? 0 : progress.getCorrectCount()) + 1);
            newScore = (short) Math.round(prevScore + (100 - prevScore) * 0.5);
        } else {
            progress.setIncorrectCount((progress.getIncorrectCount() == null ? 0 : progress.getIncorrectCount()) + 1);
            double penalty = Math.min(Math.max(1.0 / Math.max(1, exposure), 0.1), 1.0);
            newScore = (short) Math.max(0, Math.round(prevScore - (prevScore - 5) * penalty));
        }

        progress.setMasteryScore(newScore);
        progress.setExposureCount(exposure + 1);
        progress.setLastSeenAt(Instant.now());
        progress.setNextReviewAt(nextReviewAt(newScore, correct));
        progressRepository.save(progress);
        return toDto(progress);
    }

    /**
     * Простейшее SRS-планирование интервала (растёт при успехах, сжимается при
     * ошибках), согласованное с ADR-007 духом «due-review». Константы могут быть
     * уточнены при реализации (lexical-quizzes.md §6).
     */
    private Instant nextReviewAt(short score, boolean correct) {
        Instant now = Instant.now();
        if (!correct) {
            return now.plus(Duration.ofHours(1));
        }
        int masteredStrength = Math.max(1, score / MASTERED_THRESHOLD);
        long hours = switch (Math.min(masteredStrength, 6)) {
            case 1 -> 4;
            case 2 -> 24;
            case 3 -> 72;
            case 4 -> 168;
            case 5 -> 336;
            default -> 720;
        };
        return now.plus(Duration.ofHours(hours));
    }

    private LexemeProgressDto toDto(UserLexemeProgress p) {
        return new LexemeProgressDto(
                p.getId().getLexemeId(),
                p.getMasteryScore() == null ? 0 : p.getMasteryScore(),
                p.getExposureCount() == null ? 0 : p.getExposureCount(),
                p.getCorrectCount() == null ? 0 : p.getCorrectCount(),
                p.getIncorrectCount() == null ? 0 : p.getIncorrectCount(),
                p.getLastSeenAt(),
                p.getNextReviewAt());
    }
}