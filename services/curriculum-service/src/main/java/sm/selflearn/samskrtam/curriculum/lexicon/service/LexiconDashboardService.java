package sm.selflearn.samskrtam.curriculum.lexicon.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sm.selflearn.samskrtam.curriculum.lexicon.dto.LexiconDashboardResponse;
import sm.selflearn.samskrtam.curriculum.lexicon.dto.LexiconFrequencyDto;
import sm.selflearn.samskrtam.curriculum.lexicon.dto.LexiconPosDto;
import sm.selflearn.samskrtam.curriculum.lexicon.dto.LexiconSemanticClassDto;
import sm.selflearn.samskrtam.curriculum.lexicon.dto.LexiconSummaryDto;
import sm.selflearn.samskrtam.curriculum.lexicon.dto.LexiconTodayDto;
import sm.selflearn.samskrtam.curriculum.lexicon.dto.LexiconQuickStartDto;
import sm.selflearn.samskrtam.curriculum.lexicon.dto.LexiconUserCollectionDto;
import sm.selflearn.samskrtam.curriculum.lexicon.imports.LexiconImportService;
import sm.selflearn.samskrtam.curriculum.lexicon.mapper.LexiconFrequencyMapper;
import sm.selflearn.samskrtam.curriculum.lexicon.mapper.LexiconSemanticClassMapper;
import sm.selflearn.samskrtam.curriculum.lexicon.mapper.LexiconPosMapper;
import sm.selflearn.samskrtam.curriculum.lexicon.model.SemanticClass;
import sm.selflearn.samskrtam.curriculum.lexicon.model.UserLexemeProgress;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.FrequencyBandRepository;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.LexemeFrequencyRepository;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.LexemeRepository;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.SemanticClassRepository;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.PartOfSpeechRepository;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.UserCollectionRepository;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.UserLexemeProgressRepository;

import java.time.Instant;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Builds the payload for the lexicon home page (the "Лексика" dashboard).
 * Every counter comes from the real curriculum schema: total words and
 * frequency-band/topic/pos sizes are actual lexeme counts, per-user mastered
 * and "Today" counters are derived from {@code user_lexeme_progress}.
 * With {@code userId == null} (anonymous) per-user counters return zero.
 */
@Service
@RequiredArgsConstructor
public class LexiconDashboardService {

    private final FrequencyBandRepository frequencyBandRepository;
    private final SemanticClassRepository semanticClassRepository;
    private final PartOfSpeechRepository partOfSpeechRepository;
    private final LexemeRepository lexemeRepository;
    private final UserLexemeProgressRepository progressRepository;
    private final LexemeFrequencyRepository frequencyRepository;
    private final UserCollectionRepository userCollectionRepository;

    private final LexiconFrequencyMapper frequencyMapper;
    private final LexiconSemanticClassMapper semanticClassMapper;
    private final LexiconPosMapper posMapper;

    @Transactional(readOnly = true)
    public LexiconDashboardResponse getDashboard(UUID userId) {
        List<UserLexemeProgress> progress = userId == null ? List.of()
                : progressRepository.findByIdUserId(userId);

        int totalWords = (int) lexemeRepository.count();
        Set<UUID> masteredLexemeIds = masteredIds(progress);

        LexiconSummaryDto summary = new LexiconSummaryDto(totalWords, masteredLexemeIds.size());
        LexiconTodayDto today = today(progress, totalWords, userId);

        List<LexiconFrequencyDto> frequencyBands = frequencyBandRepository.findAllByOrderBySortOrderAsc()
                .stream()
                .map(band -> {
                    Set<UUID> bandIds = new HashSet<>(frequencyRepository.findLexemeIdsBySourceAndRankRange(
                            LexiconImportService.FREQUENCY_SOURCE, band.getMinRank(), band.getMaxRank()));
                    int mastered = bandIds.isEmpty() ? 0
                            : (int) bandIds.stream().filter(masteredLexemeIds::contains).count();
                    return frequencyMapper.toDto(band, bandIds.size(), mastered);
                })
                .toList();

        List<SemanticClass> classes = semanticClassRepository.findAll();
        Map<String, Integer> wordCountByCode = semanticClassRepository.findSemanticClassLexemeCounts()
                .stream()
                .collect(Collectors.toMap(SemanticClassRepository.SemanticClassLexemeCount::getCode,
                        c -> c.getLexemeCount() == null ? 0 : c.getLexemeCount().intValue()));
        Map<String, Integer> masteredByCode = masteredBySemanticClass(classes, masteredLexemeIds);

        List<LexiconSemanticClassDto> topics = classes.stream()
                .map(topic -> semanticClassMapper.toDto(
                        topic,
                        wordCountByCode.getOrDefault(topic.getCode(), 0),
                        masteredByCode.getOrDefault(topic.getCode(), 0)))
                .toList();

        Map<String, Long> posCountByCode = lexemeRepository.countLexemesByPartOfSpeech()
                .stream()
                .collect(Collectors.toMap(LexemeRepository.PosCount::getCode, LexemeRepository.PosCount::getCnt));

        List<LexiconPosDto> pos = partOfSpeechRepository.findAll()
                .stream()
                .map(part -> posMapper.toDto(part, posCountByCode.getOrDefault(part.getCode(), 0L).intValue()))
                .toList();

        List<LexiconUserCollectionDto> collections = List.of();

        List<LexiconQuickStartDto> quickStart = List.of(
                new LexiconQuickStartDto("top100", "Топ-100", "Top 100", "10 вопросов", "10 questions"),
                new LexiconQuickStartDto("top250", "Топ-250", "Top 250", "15 вопросов", "15 questions"),
                new LexiconQuickStartDto("top500", "Топ-500", "Top 500", "20 вопросов", "20 questions"));

        return new LexiconDashboardResponse(
                summary, today, frequencyBands, topics, pos, collections, quickStart);
    }

    private Set<UUID> masteredIds(List<UserLexemeProgress> progress) {
        return progress.stream()
                .filter(p -> p.getMasteryScore() != null
                        && p.getMasteryScore() >= LexemeProgressService.MASTERED_THRESHOLD)
                .map(p -> p.getId().getLexemeId())
                .collect(Collectors.toSet());
    }

    private LexiconTodayDto today(List<UserLexemeProgress> progress, int totalWords, UUID userId) {
        if (userId == null) {
            return new LexiconTodayDto(0, 0, 0);
        }
        Instant now = Instant.now();
        int reviewDue = (int) progress.stream()
                .filter(p -> p.getNextReviewAt() != null && !p.getNextReviewAt().isAfter(now))
                .count();
        int weakWords = (int) progress.stream()
                .filter(p -> p.getMasteryScore() == null
                        || p.getMasteryScore() < LexemeProgressService.MASTERED_THRESHOLD)
                .filter(p -> p.getExposureCount() != null && p.getExposureCount() > 0)
                .count();
        long seen = progress.stream()
                .filter(p -> p.getExposureCount() != null && p.getExposureCount() > 0)
                .count();
        int newWords = (int) Math.max(0, (long) totalWords - seen);
        return new LexiconTodayDto(reviewDue, newWords, weakWords);
    }

    /**
     * Mastered lexemes per semantic class, including the subtree: a lexeme bound
     * to a leaf counts into every ancestor, mirroring the
     * {@code semantic_class_lexeme_counts} view aggregation.
     */
    private Map<String, Integer> masteredBySemanticClass(
            List<SemanticClass> classes, Set<UUID> masteredLexemeIds) {
        Map<String, Integer> masteredByCode = new HashMap<>();
        if (masteredLexemeIds.isEmpty()) {
            return masteredByCode;
        }
        Map<UUID, SemanticClass> byId = classes.stream()
                .collect(Collectors.toMap(SemanticClass::getId, c -> c));
        for (UUID classId : lexemeRepository.findSemanticClassIdsByLexemeIds(masteredLexemeIds)) {
            SemanticClass node = byId.get(classId);
            while (node != null) {
                masteredByCode.merge(node.getCode(), 1, Integer::sum);
                node = node.getParent();
            }
        }
        return masteredByCode;
    }
}