package sm.selflearn.samskrtam.curriculum.lexicon.imports;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sm.selflearn.samskrtam.curriculum.lexicon.model.Lemma;
import sm.selflearn.samskrtam.curriculum.lexicon.model.LexemeGender;
import sm.selflearn.samskrtam.curriculum.lexicon.model.LemmaLexicalTopic;
import sm.selflearn.samskrtam.curriculum.lexicon.model.LemmaLexicalTopicId;
import sm.selflearn.samskrtam.curriculum.lexicon.model.LemmaTranslation;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.LemmaLexicalTopicRepository;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.LemmaRepository;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.LemmaTranslationRepository;
import sm.selflearn.samskrtam.curriculum.model.LearningLevel;
import sm.selflearn.samskrtam.curriculum.model.Topic;
import sm.selflearn.samskrtam.curriculum.model.TopicDomain;
import sm.selflearn.samskrtam.curriculum.model.TopicDomainType;
import sm.selflearn.samskrtam.curriculum.questgen.LexicalQuizItemGenerator;
import sm.selflearn.samskrtam.curriculum.questitem.repository.QuestItemRepository;
import sm.selflearn.samskrtam.curriculum.repository.TopicRepository;
import sm.selflearn.samskrtam.quest.VocabularyQuestItemTypes;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VerseLexemeImportService {

    private final LemmaRepository lemmaRepository;
    private final LemmaTranslationRepository lemmaTranslationRepository;
    private final LemmaLexicalTopicRepository lemmaLexicalTopicRepository;
    private final TopicRepository topicRepository;
    private final QuestItemRepository questItemRepository;
    private final LexicalQuizItemGenerator lexicalQuizItemGenerator;

    @Transactional
    public VerseBatchImportResult importVerseBatch(VerseLemmaBatchRequest request) {
        List<LemmaExportItem> uniqueWords = dedupe(request.words());

        int imported = 0;
        int updated = 0;
        List<String> batchLemmas = new ArrayList<>();

        for (LemmaExportItem word : uniqueWords) {
            if (isBlank(word.lemmaIast())) {
                continue;
            }
            LexemeGender gender = parseGender(word.gender());
            String pos = word.dominantPosCode();
            Lemma lemma = findOrCreateLemma(word.lemmaIast(), pos, gender);
            boolean createdRu = upsertTranslation(lemma, "ru", word.glossRu());
            boolean createdEn = upsertTranslation(lemma, "en", word.glossEn());
            if (createdRu || createdEn) {
                imported++;
            } else {
                updated++;
            }
            batchLemmas.add(word.lemmaIast());
        }

        Topic topic = ensureVerseTopic(request);
        for (String lemmaIast : batchLemmas) {
            bind(topic.getCode(), lemmaIast);
        }

        regenerateVerseTopicItems(topic);

        log.info("Verse batch processed: verse={}, imported={}, updated={}, topicCode={}",
                request.verseId(), imported, updated, topic.getCode());
        return new VerseBatchImportResult(imported, updated, topic.getId(), topic.getCode());
    }

    private List<LemmaExportItem> dedupe(List<LemmaExportItem> words) {
        if (words == null || words.isEmpty()) {
            return List.of();
        }
        Map<String, LemmaExportItem> unique = new LinkedHashMap<>();
        for (LemmaExportItem word : words) {
            if (isBlank(word.lemmaIast())) {
                continue;
            }
            unique.putIfAbsent(word.lemmaIast(), word);
        }
        return List.copyOf(unique.values());
    }

    private Lemma findOrCreateLemma(String lemmaIast, String pos, LexemeGender gender) {
        return lemmaRepository.findByLemmaIast(lemmaIast)
                .orElseGet(() -> {
                    Lemma lemma = new Lemma();
                    lemma.setLemmaIast(lemmaIast);
                    lemma.setPos(pos);
                    lemma.setGender(gender);
                    return lemmaRepository.save(lemma);
                });
    }

    private boolean upsertTranslation(Lemma lemma, String language, String gloss) {
        if (isBlank(gloss)) {
            return false;
        }
        if (!lemmaTranslationRepository.findByLemma_LemmaIastAndLanguage(lemma.getLemmaIast(), language).isEmpty()) {
            return false;
        }
        LemmaTranslation translation = new LemmaTranslation();
        translation.setLemma(lemma);
        translation.setLanguage(language);
        translation.setGloss(gloss.length() > 300 ? gloss.substring(0, 300) : gloss);
        translation.setMain(true);
        lemmaTranslationRepository.save(translation);
        return true;
    }

    private void bind(String topicCode, String lemmaIast) {
        LemmaLexicalTopicId key = new LemmaLexicalTopicId();
        key.setTopicCode(topicCode);
        key.setLemmaIast(lemmaIast);
        if (lemmaLexicalTopicRepository.existsById(key)) {
            return;
        }
        LemmaLexicalTopic binding = new LemmaLexicalTopic();
        binding.setId(key);
        lemmaLexicalTopicRepository.save(binding);
    }

    private Topic ensureVerseTopic(VerseLemmaBatchRequest request) {
        String code = topicCode(request);
        Topic topic = topicRepository.findByCode(code).orElse(null);

        String titleRu;
        String titleEn;
        if (request.ownerId() != null) {
            titleRu = "Мои слова";
            titleEn = "My words";
        } else {
            String baseRu = blankTo(request.workTitleRu(), request.workSlug());
            String baseEn = blankTo(request.workTitleEn(), request.workSlug());
            titleRu = "Слова «" + baseRu + "», гл. " + request.chapterNumber();
            titleEn = baseEn + " — ch. " + request.chapterNumber();
        }

        if (topic == null) {
            topic = new Topic();
            topic.setCode(code);
            topic.setDomain(TopicDomain.VERSE);
            topic.setDomainType(TopicDomainType.VERSE);
            topic.setLearningLevel(LearningLevel.L0);
            topic.setEvergreen(false);
            topic.setTitleRu(titleRu);
            topic.setTitleEn(titleEn);
            topicRepository.save(topic);
        } else {
            topic.setTitleRu(titleRu);
            topic.setTitleEn(titleEn);
        }
        return topic;
    }

    private String topicCode(VerseLemmaBatchRequest request) {
        if (request.ownerId() != null) {
            return "user-" + request.ownerId();
        }
        String base = isBlank(request.workSlp1()) ? "verse" : request.workSlp1();
        return base + "_" + request.chapterNumber();
    }

    private void regenerateVerseTopicItems(Topic topic) {
        questItemRepository.deleteByTopicIdAndItemType(topic.getId(),
                VocabularyQuestItemTypes.VOCABULARY_WORD.code());
        lexicalQuizItemGenerator.generate(topic);
    }

    private static String blankTo(String value, String fallback) {
        return isBlank(value) ? (fallback == null ? "" : fallback) : value;
    }

    static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    static LexemeGender parseGender(String gender) {
        if (gender == null) {
            return null;
        }
        try {
            return LexemeGender.valueOf(gender);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}