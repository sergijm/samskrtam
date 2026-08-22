package sm.selflearn.samskrtam.curriculum.lexicon.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import sm.selflearn.samskrtam.curriculum.lexicon.dto.LexiconDashboardResponse;
import sm.selflearn.samskrtam.curriculum.lexicon.dto.LexiconDashboardResponse.FrequencyBand;
import sm.selflearn.samskrtam.curriculum.lexicon.dto.LexiconDashboardResponse.LexicalTopic;
import sm.selflearn.samskrtam.curriculum.lexicon.dto.LexiconDashboardResponse.LexiconPos;
import sm.selflearn.samskrtam.curriculum.lexicon.dto.LexiconDashboardResponse.QuickStartPreset;
import sm.selflearn.samskrtam.curriculum.lexicon.dto.LexiconDashboardResponse.UserCollection;
import sm.selflearn.samskrtam.curriculum.lexicon.model.PartOfSpeech;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.FrequencyBandRepository;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.LemmaLexicalTopicRepository;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.LemmaTranslationRepository;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.PartOfSpeechRepository;
import sm.selflearn.samskrtam.curriculum.model.Topic;
import sm.selflearn.samskrtam.curriculum.repository.TopicRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LexiconDashboardService {

    private final LemmaTranslationRepository lemmaTranslationRepository;
    private final LemmaLexicalTopicRepository lemmaLexicalTopicRepository;
    private final FrequencyBandRepository frequencyBandRepository;
    private final TopicRepository topicRepository;
    private final PartOfSpeechRepository partOfSpeechRepository;

    public LexiconDashboardResponse getDashboard() {
        long totalDistinctLemmas = lemmaTranslationRepository.countDistinctLemmaIast();

        List<FrequencyBand> frequencyBands = frequencyBandRepository.findAllByOrderBySortOrderAsc().stream()
                .map(this::toBand)
                .toList();

        List<LexicalTopic> topics = lemmaLexicalTopicRepository.countLemmasByTopicCode().stream()
                .map(r -> toTopic((String) r[0], ((Number) r[1]).longValue()))
                .toList();

        Map<String, PartOfSpeech> posByName = posIndex();
        List<LexiconPos> pos = lemmaTranslationRepository.countDistinctLemmasByPos().stream()
                .map(r -> toPos((String) r[0], ((Number) r[1]).longValue(), posByName))
                .toList();

        return new LexiconDashboardResponse(
                new LexiconDashboardResponse.LexiconProgressSummary(totalDistinctLemmas, 0L),
                new LexiconDashboardResponse.LexiconToday(0L, 0L, 0L),
                frequencyBands,
                topics,
                pos,
                List.of(),
                List.of());
    }

    private FrequencyBand toBand(sm.selflearn.samskrtam.curriculum.lexicon.model.FrequencyBand band) {
        long wordCount = lemmaTranslationRepository
                .findDistinctLemmaIastByFrequencyRankRange(band.getMinRank(), band.getMaxRank())
                .size();
        return new FrequencyBand(band.getCode(), band.getMinRank(), band.getMaxRank(), wordCount, 0L);
    }

    private LexicalTopic toTopic(String topicCode, long wordCount) {
        Optional<Topic> topic = topicRepository.findByCode(topicCode);
        String nameRu = topic.map(Topic::getTitleRu).orElse(topicCode);
        String nameEn = topic.map(Topic::getTitleEn).orElse(topicCode);
        return new LexicalTopic(topicCode, nameRu, nameEn, wordCount, 0L);
    }

    private Map<String, PartOfSpeech> posIndex() {
        return partOfSpeechRepository.findAll().stream()
                .collect(java.util.stream.Collectors.toMap(PartOfSpeech::getCode, p -> p, (a, b) -> a));
    }

    private LexiconPos toPos(String posCode, long wordCount, Map<String, PartOfSpeech> posByName) {
        PartOfSpeech ps = posByName.get(posCode);
        if (ps == null) {
            ps = posByName.get(posCode.toLowerCase());
        }
        String nameRu = ps != null ? ps.getNameRu() : posCode;
        String nameEn = ps != null ? ps.getNameEn() : posCode;
        return new LexiconPos(posCode, nameRu, nameEn, wordCount);
    }
}
