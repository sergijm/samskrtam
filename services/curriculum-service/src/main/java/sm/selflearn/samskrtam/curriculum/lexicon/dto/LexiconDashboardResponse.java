package sm.selflearn.samskrtam.curriculum.lexicon.dto;

import java.util.List;

/**
 * Payload for the lexicon home page, mirroring the grid the frontend
 * `LexiconPage` renders: hero summary, Today, frequency bands, semantic classes,
 * parts of speech, sources and user collections. All counters are real counts
 * from the curriculum schema; per-user mastered / Today counters come from
 * {@code user_lexeme_progress} and are zero without {@code X-User-Id}.
 */
public record LexiconDashboardResponse(
        LexiconSummaryDto summary,
        LexiconTodayDto today,
        List<LexiconFrequencyDto> frequencyBands,
        List<LexiconSemanticClassDto> topics,
        List<LexiconPosDto> pos,
        List<LexiconUserCollectionDto> collections,
        List<LexiconQuickStartDto> quickStart
) {
}