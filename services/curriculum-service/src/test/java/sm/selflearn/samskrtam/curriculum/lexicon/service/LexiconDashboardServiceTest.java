package sm.selflearn.samskrtam.curriculum.lexicon.service;

import org.junit.jupiter.api.Test;
import sm.selflearn.samskrtam.curriculum.lexicon.dto.LexiconDashboardResponse;
import sm.selflearn.samskrtam.curriculum.lexicon.dto.LexiconFrequencyDto;
import sm.selflearn.samskrtam.curriculum.lexicon.dto.LexiconPosDto;
import sm.selflearn.samskrtam.curriculum.lexicon.dto.LexiconSemanticTopicDto;
import sm.selflearn.samskrtam.curriculum.lexicon.dto.LexiconSourceDto;
import sm.selflearn.samskrtam.curriculum.lexicon.mapper.LexiconFrequencyMapperImpl;
import sm.selflearn.samskrtam.curriculum.lexicon.mapper.LexiconSemanticTopicMapperImpl;
import sm.selflearn.samskrtam.curriculum.lexicon.mapper.LexiconPosMapperImpl;
import sm.selflearn.samskrtam.curriculum.lexicon.mapper.LexiconSourceMapperImpl;
import sm.selflearn.samskrtam.curriculum.lexicon.model.FrequencyBand;
import sm.selflearn.samskrtam.curriculum.lexicon.model.PartOfSpeech;
import sm.selflearn.samskrtam.curriculum.lexicon.model.PosGroup;
import sm.selflearn.samskrtam.curriculum.lexicon.model.SemanticTopic;
import sm.selflearn.samskrtam.curriculum.lexicon.model.Source;
import sm.selflearn.samskrtam.curriculum.lexicon.model.SourceKind;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.FrequencyBandRepository;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.SemanticTopicRepository;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.PartOfSpeechRepository;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.SourceRepository;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.UserCollectionRepository;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LexiconDashboardServiceTest {

    private LexiconDashboardService service(
            FrequencyBandRepository bands,
            SemanticTopicRepository topics,
            PartOfSpeechRepository pos,
            SourceRepository sources,
            UserCollectionRepository collections) {
        return new LexiconDashboardService(
                bands, topics, pos, sources, collections,
                new LexiconFrequencyMapperImpl(),
                new LexiconSemanticTopicMapperImpl(),
                new LexiconPosMapperImpl(),
                new LexiconSourceMapperImpl());
    }

    private FrequencyBand band(String code, int min, int max) {
        FrequencyBand b = new FrequencyBand();
        b.setCode(code);
        b.setMinRank(min);
        b.setMaxRank(max);
        b.setSortOrder((short) 0);
        return b;
    }

    private SemanticTopic topic(String code, String ru, String en) {
        SemanticTopic t = new SemanticTopic();
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

    private Source source(String code) {
        Source s = new Source();
        s.setId(UUID.randomUUID());
        s.setTitleRu(code);
        s.setTitleEn(code);
        s.setKind(SourceKind.EPIC);
        s.setUniqueLemmaCountCache(400);
        return s;
    }

    @Test
    void getDashboard_mapsTaxonomyAndReportsRandomProgress() {
        FrequencyBandRepository bands = mock(FrequencyBandRepository.class);
        SemanticTopicRepository topics = mock(SemanticTopicRepository.class);
        PartOfSpeechRepository pos = mock(PartOfSpeechRepository.class);
        SourceRepository sources = mock(SourceRepository.class);
        UserCollectionRepository collections = mock(UserCollectionRepository.class);

        when(bands.findAllByOrderBySortOrderAsc())
                .thenReturn(List.of(band("CORE", 1, 100), band("ESSENTIAL", 101, 250)));
        when(topics.findAll()).thenReturn(List.of(topic("nature", "Природа", "Nature")));
        when(pos.findAll()).thenReturn(List.of(pos("noun")));
        when(sources.findAll()).thenReturn(List.of(source("gita")));

        LexiconDashboardResponse response =
                service(bands, topics, pos, sources, collections).getDashboard();

        assertThat(response.summary().totalWords()).isEqualTo(LexiconDashboardService.TOTAL_WORDS);
        assertThat(response.summary().masteredCount())
                .isBetween(0, LexiconDashboardService.TOTAL_WORDS);

        assertThat(response.frequencyBands()).hasSize(2);
        LexiconFrequencyDto core = response.frequencyBands().get(0);
        assertThat(core.id()).isEqualTo("CORE");
        assertThat(core.wordCount()).isEqualTo(100);
        assertThat(core.masteredCount()).isBetween(0, 100);

        assertThat(response.topics()).extracting(LexiconSemanticTopicDto::id)
                .containsExactly("nature");
        assertThat(response.topics().get(0).nameRu()).isEqualTo("Природа");
        assertThat(response.topics().get(0).nameEn()).isEqualTo("Nature");

        assertThat(response.pos()).extracting(LexiconPosDto::id).containsExactly("noun");

        LexiconSourceDto gita = response.sources().get(0);
        assertThat(gita.wordCount()).isEqualTo(400);
        assertThat(gita.titleEn()).isEqualTo("gita");

        assertThat(response.quickStart()).hasSize(3);
        assertThat(response.collections()).isEmpty();
    }
}