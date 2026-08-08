package sm.selflearn.samskrtam.curriculum.lexicon.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sm.selflearn.samskrtam.curriculum.lexicon.dto.LexiconDashboardResponse;
import sm.selflearn.samskrtam.curriculum.lexicon.dto.LexiconFrequencyDto;
import sm.selflearn.samskrtam.curriculum.lexicon.dto.LexiconPosDto;
import sm.selflearn.samskrtam.curriculum.lexicon.dto.LexiconSemanticTopicDto;
import sm.selflearn.samskrtam.curriculum.lexicon.dto.LexiconSourceDto;
import sm.selflearn.samskrtam.curriculum.lexicon.dto.LexiconSummaryDto;
import sm.selflearn.samskrtam.curriculum.lexicon.dto.LexiconTodayDto;
import sm.selflearn.samskrtam.curriculum.lexicon.dto.LexiconQuickStartDto;
import sm.selflearn.samskrtam.curriculum.lexicon.dto.LexiconUserCollectionDto;
import sm.selflearn.samskrtam.curriculum.lexicon.mapper.LexiconFrequencyMapper;
import sm.selflearn.samskrtam.curriculum.lexicon.mapper.LexiconSemanticTopicMapper;
import sm.selflearn.samskrtam.curriculum.lexicon.mapper.LexiconPosMapper;
import sm.selflearn.samskrtam.curriculum.lexicon.mapper.LexiconSourceMapper;
import sm.selflearn.samskrtam.curriculum.lexicon.model.FrequencyBand;
import sm.selflearn.samskrtam.curriculum.lexicon.model.PartOfSpeech;
import sm.selflearn.samskrtam.curriculum.lexicon.model.SemanticTopic;
import sm.selflearn.samskrtam.curriculum.lexicon.model.Source;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.FrequencyBandRepository;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.SemanticTopicRepository;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.PartOfSpeechRepository;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.SourceRepository;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.UserCollectionRepository;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Builds the payload for the lexicon home page (the "Лексика" dashboard).
 * Taxonomy (frequency bands, semantic topics, parts of speech, sources) comes
 * from the real curriculum schema; per-user progress (mastered counts, Today
 * counters) is currently random — real spaced-repetition state is an open
 * question (see docs/docs/services/lexical-quizzes.md §4).
 */
@Service
@RequiredArgsConstructor
public class LexiconDashboardService {

    public static final int TOTAL_WORDS = 2000;

    private final FrequencyBandRepository frequencyBandRepository;
    private final SemanticTopicRepository semanticTopicRepository;
    private final PartOfSpeechRepository partOfSpeechRepository;
    private final SourceRepository sourceRepository;
    private final UserCollectionRepository userCollectionRepository;

    private final LexiconFrequencyMapper frequencyMapper;
    private final LexiconSemanticTopicMapper semanticTopicMapper;
    private final LexiconPosMapper posMapper;
    private final LexiconSourceMapper sourceMapper;

    @Transactional(readOnly = true)
    public LexiconDashboardResponse getDashboard() {
        ThreadLocalRandom random = ThreadLocalRandom.current();

        int todayReview = random.nextInt(0, 40);
        int todayNew = random.nextInt(0, 20);
        int todayWeak = random.nextInt(0, 12);

        LexiconSummaryDto summary = new LexiconSummaryDto(
                TOTAL_WORDS,
                random.nextInt(0, TOTAL_WORDS + 1));
        LexiconTodayDto today = new LexiconTodayDto(todayReview, todayNew, todayWeak);

        List<LexiconFrequencyDto> frequencyBands = frequencyBandRepository.findAllByOrderBySortOrderAsc()
                .stream()
                .map(band -> {
                    int bandSize = Math.max(band.getMaxRank() - band.getMinRank() + 1, 0);
                    int mastered = random.nextInt(0, bandSize + 1);
                    return frequencyMapper.toDto(band, bandSize, mastered);
                })
                .toList();

        List<LexiconSemanticTopicDto> topics = semanticTopicRepository.findAll()
                .stream()
                .map(topic -> semanticTopicMapper.toDto(
                        topic, random.nextInt(10, 160), random.nextInt(0, 160)))
                .toList();

        List<LexiconPosDto> pos = partOfSpeechRepository.findAll()
                .stream()
                .map(part -> posMapper.toDto(part, random.nextInt(20, 700)))
                .toList();

        List<LexiconSourceDto> sources = sourceRepository.findAll()
                .stream()
                .map(source -> sourceMapper.toDto(source, random.nextInt(0, 500)))
                .toList();

        // Collections are per-user; the read-only dashboard has no user context
        // yet, so an empty set is returned. Populated when auth wiring arrives.
        List<LexiconUserCollectionDto> collections = List.of();

        List<LexiconQuickStartDto> quickStart = List.of(
                new LexiconQuickStartDto("top100", "Топ-100", "Top 100", "10 вопросов", "10 questions"),
                new LexiconQuickStartDto("top250", "Топ-250", "Top 250", "15 вопросов", "15 questions"),
                new LexiconQuickStartDto("top500", "Топ-500", "Top 500", "20 вопросов", "20 questions"));

        return new LexiconDashboardResponse(
                summary, today, frequencyBands, topics, pos, sources, collections, quickStart);
    }
}