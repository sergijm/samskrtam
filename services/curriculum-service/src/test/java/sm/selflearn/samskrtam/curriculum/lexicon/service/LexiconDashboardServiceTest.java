package sm.selflearn.samskrtam.curriculum.lexicon.service;

import org.junit.jupiter.api.Test;
import sm.selflearn.samskrtam.curriculum.lexicon.dto.LexiconDashboardResponse;
import sm.selflearn.samskrtam.curriculum.lexicon.dto.LexiconFrequencyDto;
import sm.selflearn.samskrtam.curriculum.lexicon.dto.LexiconPosDto;
import sm.selflearn.samskrtam.curriculum.lexicon.dto.LexiconSemanticClassDto;
import sm.selflearn.samskrtam.curriculum.lexicon.mapper.LexiconFrequencyMapperImpl;
import sm.selflearn.samskrtam.curriculum.lexicon.mapper.LexiconSemanticClassMapperImpl;
import sm.selflearn.samskrtam.curriculum.lexicon.mapper.LexiconPosMapperImpl;
import sm.selflearn.samskrtam.curriculum.lexicon.model.FrequencyBand;
import sm.selflearn.samskrtam.curriculum.lexicon.model.PartOfSpeech;
import sm.selflearn.samskrtam.curriculum.lexicon.model.PosGroup;
import sm.selflearn.samskrtam.curriculum.lexicon.model.SemanticClass;
import sm.selflearn.samskrtam.curriculum.lexicon.model.UserLexemeProgress;
import sm.selflearn.samskrtam.curriculum.lexicon.model.UserLexemeProgressId;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.FrequencyBandRepository;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.LexemeFrequencyRepository;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.LexemeRepository;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.SemanticClassRepository;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.PartOfSpeechRepository;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.UserCollectionRepository;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.UserLexemeProgressRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LexiconDashboardServiceTest {

    private static final UUID USER = UUID.randomUUID();
    private static final UUID MASTERED_LEXEME = UUID.randomUUID();
    private static final UUID WEAK_LEXEME = UUID.randomUUID();
    private static final UUID DUE_LEXEME = UUID.randomUUID();

    private LexiconDashboardService service(
            FrequencyBandRepository bands,
            SemanticClassRepository topics,
            PartOfSpeechRepository pos,
            UserCollectionRepository collections,
            LexemeRepository lexemes,
            UserLexemeProgressRepository progress,
            LexemeFrequencyRepository frequencies) {
        return new LexiconDashboardService(
                bands, topics, pos, lexemes, progress, frequencies, collections,
                new LexiconFrequencyMapperImpl(),
                new LexiconSemanticClassMapperImpl(),
                new LexiconPosMapperImpl());
    }

    private FrequencyBand band(String code, int min, int max) {
        FrequencyBand b = new FrequencyBand();
        b.setCode(code);
        b.setMinRank(min);
        b.setMaxRank(max);
        b.setSortOrder((short) 0);
        return b;
    }

    private SemanticClass topic(String code, String ru, String en) {
        SemanticClass t = new SemanticClass();
        t.setId(UUID.randomUUID());
        t.setCode(code);
        t.setNameRu(ru);
        t.setNameEn(en);
        return t;
    }

    private PartOfSpeech pos(String code) {
        PartOfSpeech p = new PartOfSpeech();
        p.setCode(code);
        p.setGroup(PosGroup.NOMINAL);
        p.setNameRu(code);
        p.setNameEn(code);
        return p;
    }

    private UserLexemeProgress progress(UUID lexeme, short score, int exposure, Instant nextReview) {
        UserLexemeProgress p = new UserLexemeProgress();
        UserLexemeProgressId id = new UserLexemeProgressId();
        id.setUserId(USER);
        id.setLexemeId(lexeme);
        p.setId(id);
        p.setMasteryScore(score);
        p.setExposureCount(exposure);
        p.setNextReviewAt(nextReview);
        return p;
    }

    @Test
    void getDashboard_returnsRealCountsFromRepositories() {
        FrequencyBandRepository bands = mock(FrequencyBandRepository.class);
        SemanticClassRepository topics = mock(SemanticClassRepository.class);
        PartOfSpeechRepository pos = mock(PartOfSpeechRepository.class);
        UserCollectionRepository collections = mock(UserCollectionRepository.class);
        LexemeRepository lexemes = mock(LexemeRepository.class);
        UserLexemeProgressRepository progress = mock(UserLexemeProgressRepository.class);
        LexemeFrequencyRepository frequencies = mock(LexemeFrequencyRepository.class);

        SemanticClass movement = topic("movement-action", "Движение и действие", "Movement and action");
        when(topics.findAll()).thenReturn(List.of(movement));

        semanticCount(topics, "movement-action", 139);

        posCount(lexemes, "noun", 120);

        when(bands.findAllByOrderBySortOrderAsc())
                .thenReturn(List.of(band("CORE", 1, 100), band("ESSENTIAL", 101, 250)));
        when(frequencies.findLexemeIdsBySourceAndRankRange(eq("SANGRAHA_CORPUS"), eq(1), eq(100)))
                .thenReturn(List.of(MASTERED_LEXEME, WEAK_LEXEME, DUE_LEXEME));
        when(frequencies.findLexemeIdsBySourceAndRankRange(eq("SANGRAHA_CORPUS"), eq(101), eq(250)))
                .thenReturn(List.of());

        when(lexemes.count()).thenReturn(5L);
        when(progress.findByIdUserId(USER)).thenReturn(List.of(
                progress(MASTERED_LEXEME, (short) 95, 3, Instant.now().minusSeconds(60)),
                progress(WEAK_LEXEME, (short) 50, 2, Instant.now().plusSeconds(3600)),
                progress(DUE_LEXEME, (short) 92, 5, Instant.now().minusSeconds(120))));

        when(lexemes.findSemanticClassIdsByLexemeIds(any())).thenReturn(List.of(movement.getId()));

        when(pos.findAll()).thenReturn(List.of(pos("noun")));

        LexiconDashboardResponse response =
                service(bands, topics, pos, collections, lexemes, progress, frequencies)
                        .getDashboard(USER);

        assertThat(response.summary().totalWords()).isEqualTo(5);
        assertThat(response.summary().masteredCount()).isEqualTo(2);

        assertThat(response.today().reviewDue()).isEqualTo(2);
        assertThat(response.today().weakWords()).isEqualTo(1);
        assertThat(response.today().newWords()).isEqualTo(2);

        LexiconFrequencyDto core = response.frequencyBands().get(0);
        assertThat(core.id()).isEqualTo("CORE");
        assertThat(core.wordCount()).isEqualTo(3);
        assertThat(core.masteredCount()).isEqualTo(2);
        assertThat(core.masteredCount()).isLessThanOrEqualTo(core.wordCount());

        LexiconFrequencyDto empty = response.frequencyBands().get(1);
        assertThat(empty.wordCount()).isZero();
        assertThat(empty.masteredCount()).isZero();

        assertThat(response.topics()).extracting(LexiconSemanticClassDto::id)
                .containsExactly("movement-action");
        LexiconSemanticClassDto topicDto = response.topics().get(0);
        assertThat(topicDto.wordCount()).isEqualTo(139);
        assertThat(topicDto.masteredCount()).isEqualTo(1);
        assertThat(topicDto.masteredCount()).isLessThanOrEqualTo(topicDto.wordCount());

        assertThat(response.pos()).extracting(LexiconPosDto::id).containsExactly("noun");
        assertThat(response.pos().get(0).wordCount()).isEqualTo(120);

        assertThat(response.quickStart()).hasSize(3);
        assertThat(response.collections()).isEmpty();
    }

    @Test
    void getDashboard_withoutUser_returnsZeroPerUserCounters() {
        FrequencyBandRepository bands = mock(FrequencyBandRepository.class);
        SemanticClassRepository topics = mock(SemanticClassRepository.class);
        PartOfSpeechRepository pos = mock(PartOfSpeechRepository.class);
        UserCollectionRepository collections = mock(UserCollectionRepository.class);
        LexemeRepository lexemes = mock(LexemeRepository.class);
        UserLexemeProgressRepository progress = mock(UserLexemeProgressRepository.class);
        LexemeFrequencyRepository frequencies = mock(LexemeFrequencyRepository.class);

        when(bands.findAllByOrderBySortOrderAsc()).thenReturn(List.of(band("CORE", 1, 100)));
        when(topics.findAll()).thenReturn(List.of(topic("nature", "Природа", "Nature")));
        semanticCount(topics, "nature", 100);
        posCount(lexemes, "noun", 0);
        when(lexemes.count()).thenReturn(100L);

        LexiconDashboardResponse response =
                service(bands, topics, pos, collections, lexemes, progress, frequencies).getDashboard(null);

        assertThat(response.summary().masteredCount()).isZero();
        assertThat(response.today().reviewDue()).isZero();
        assertThat(response.today().newWords()).isZero();
        assertThat(response.today().weakWords()).isZero();
        assertThat(response.frequencyBands().get(0).masteredCount()).isZero();
        assertThat(response.topics().get(0).masteredCount()).isZero();
    }

    private static void semanticCount(SemanticClassRepository topics, String code, long count) {
        SemanticClassRepository.SemanticClassLexemeCount c =
                mock(SemanticClassRepository.SemanticClassLexemeCount.class);
        when(c.getCode()).thenReturn(code);
        when(c.getLexemeCount()).thenReturn(count);
        when(topics.findSemanticClassLexemeCounts()).thenReturn(List.of(c));
    }

    private static void posCount(LexemeRepository lexemes, String code, long count) {
        LexemeRepository.PosCount pc = mock(LexemeRepository.PosCount.class);
        when(pc.getCode()).thenReturn(code);
        when(pc.getCnt()).thenReturn(count);
        when(lexemes.countLexemesByPartOfSpeech()).thenReturn(List.of(pc));
    }
}