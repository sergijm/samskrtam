package sm.selflearn.samskrtam.curriculum.lexicon.dto;

import java.util.List;

/**
 * Payload for the lexicon home page, mirroring the grid the frontend
 * `LexiconPage` renders: hero summary, Today, frequency bands, semantic topics,
 * parts of speech, sources and user collections. Per-user progress (mastered /
 * Today counters) is currently random — real tracking is an open question.
 */
public record LexiconDashboardResponse(
        LexiconSummaryDto summary,
        LexiconTodayDto today,
        List<LexiconFrequencyDto> frequencyBands,
        List<LexiconSemanticTopicDto> topics,
        List<LexiconPosDto> pos,
        List<LexiconSourceDto> sources,
        List<LexiconUserCollectionDto> collections,
        List<LexiconQuickStartDto> quickStart
) {
}