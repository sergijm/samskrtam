package sm.selflearn.samskrtam.curriculum.lexicon.imports;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sm.selflearn.samskrtam.curriculum.lexicon.model.Lexeme;
import sm.selflearn.samskrtam.curriculum.lexicon.model.LexemeGender;
import sm.selflearn.samskrtam.curriculum.lexicon.model.LexicalTopicBinding;
import sm.selflearn.samskrtam.curriculum.lexicon.model.LexicalTopicBindingId;
import sm.selflearn.samskrtam.curriculum.lexicon.model.MorphologyClass;
import sm.selflearn.samskrtam.curriculum.lexicon.model.PartOfSpeech;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.LexemeRepository;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.LexicalTopicBindingRepository;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.MorphologyClassRepository;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.PartOfSpeechRepository;
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

/**
 * Приём инкрементальной пачки лемм одного стиха от sangraha-service
 * (lexicon-content-pipeline.md §7). Для каждого слова: upsert лексемы по
 * идентичности значения (lemmaSlp1, gender, нормализованный gloss) с
 * {@code meaningNumber = max+1} для новых значений. Затем создаётся/обновляется
 * лексический урок (Topic.domain = VERSE, code = "{workSlp1}_{chapterNumber}") и
 * привязываются лексемы пачки (lexical_topic_binding). Частотность пачками не
 * ведётся (§7 шаг 2).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VerseLexemeImportService {

    private final LexemeRepository lexemeRepository;
    private final PartOfSpeechRepository partOfSpeechRepository;
    private final MorphologyClassRepository morphologyClassRepository;
    private final TopicRepository topicRepository;
    private final LexicalTopicBindingRepository bindingRepository;
    private final QuestItemRepository questItemRepository;
    private final LexicalQuizItemGenerator lexicalQuizItemGenerator;

    @Transactional
    public VerseBatchImportResult importVerseBatch(VerseLemmaBatchRequest request) {
        List<LemmaExportItem> uniqueWords = dedupe(request.words());

        int imported = 0;
        int updated = 0;
        List<UUID> batchLexemeIds = new ArrayList<>();

        for (LemmaExportItem word : uniqueWords) {
            if (LexiconImportService.isBlank(word.lemmaSlp1())) {
                continue;
            }
            UpsertResult result = upsert(word);
            if (result.lexeme() == null || result.lexeme().getId() == null) {
                continue;
            }
            if (result.created()) {
                imported++;
            } else {
                updated++;
            }
            batchLexemeIds.add(result.lexeme().getId());
        }

        Topic topic = ensureVerseTopic(request);
        attachBindings(topic.getId(), batchLexemeIds);

        regenerateVerseTopicItems(topic);

        log.info("Verse batch processed: verse={}, imported={}, updated={}, topicCode={}",
                request.verseId(), imported, updated, topic.getCode());
        return new VerseBatchImportResult(imported, updated, topic.getId(), topic.getCode());
    }

    /**
     * Дедуп внутри пачки: один (lemmaSlp1, gender) → один upsert.
     */
    private List<LemmaExportItem> dedupe(List<LemmaExportItem> words) {
        if (words == null || words.isEmpty()) {
            return List.of();
        }
        Map<String, LemmaExportItem> unique = new LinkedHashMap<>();
        for (LemmaExportItem word : words) {
            String key = word.lemmaSlp1() + "|" + (word.gender() == null ? "" : word.gender());
            unique.putIfAbsent(key, word);
        }
        return List.copyOf(unique.values());
    }

    private UpsertResult upsert(LemmaExportItem word) {
        LexemeGender gender = LexiconImportService.parseGender(word.gender());
        String glossRu = emptyIfNull(word.glossRu());
        String glossEn = emptyIfNull(word.glossEn());

        List<Lexeme> existing = lexemeRepository
                .findByLemmaSlp1AndGenderOrderByMeaningNumberAsc(word.lemmaSlp1(), gender);
        Lexeme match = existing.stream()
                .filter(lexeme -> matchesGloss(lexeme.getGlossRu(), glossRu)
                        && matchesGloss(lexeme.getGlossEn(), glossEn))
                .findFirst()
                .orElse(null);

        if (match != null) {
            if (LexiconImportService.isBlank(match.getGlossRu())) {
                match.setGlossRu(glossRu);
            }
            if (LexiconImportService.isBlank(match.getGlossEn())) {
                match.setGlossEn(glossEn);
            }
            attachPartOfSpeech(match, LexiconImportService.mapPos(word.dominantPosCode()));
            attachMorphology(match, LexiconImportService.mapMorphologyCode(word.gender(), word.vowelType()));
            return new UpsertResult(match, false);
        }

        Lexeme lexeme = new Lexeme();
        lexeme.setLemmaSlp1(word.lemmaSlp1());
        lexeme.setLemmaIast(word.lemmaIast());
        lexeme.setLemmaDevanagari(word.lemmaDevanagari());
        lexeme.setGlossRu(glossRu);
        lexeme.setGlossEn(glossEn);
        lexeme.setGender(gender);
        lexeme.setMeaningNumber(lexemeRepository.findMaxMeaningNumber(word.lemmaSlp1()) + 1);
        lexeme = lexemeRepository.save(lexeme);
        attachPartOfSpeech(lexeme, LexiconImportService.mapPos(word.dominantPosCode()));
        attachMorphology(lexeme, LexiconImportService.mapMorphologyCode(word.gender(), word.vowelType()));
        return new UpsertResult(lexeme, true);
    }

    private record UpsertResult(Lexeme lexeme, boolean created) {
    }

    private static boolean matchesGloss(String existing, String incoming) {
        // Пустой входящий gloss — wildcard (lexicon.md §1); иначе сравнение левого и правого часть равнозначно.
        if (LexiconImportService.isBlank(incoming)) {
            return true;
        }
        return existing != null && existing.trim().equalsIgnoreCase(incoming.trim());
    }

    private void attachPartOfSpeech(Lexeme lexeme, String posCode) {
        if (posCode == null) {
            return;
        }
        if (lexeme.getPartsOfSpeech().stream().anyMatch(p -> p.getCode().equals(posCode))) {
            return;
        }
        PartOfSpeech pos = partOfSpeechRepository.findByCode(posCode).orElse(null);
        if (pos == null) {
            return;
        }
        lexeme.getPartsOfSpeech().add(pos);
    }

    private void attachMorphology(Lexeme lexeme, String code) {
        if (code == null) {
            return;
        }
        if (lexeme.getMorphologyClasses().stream().anyMatch(m -> m.getCode().equals(code))) {
            return;
        }
        MorphologyClass mc = morphologyClassRepository.findByCode(code).orElse(null);
        if (mc == null) {
            return;
        }
        lexeme.getMorphologyClasses().add(mc);
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
        String base = LexiconImportService.isBlank(request.workSlp1()) ? "verse" : request.workSlp1();
        return base + "_" + request.chapterNumber();
    }

    /**
     * Привязка лексем пачки к уроку главы. Аккумулирует по стихам главы
     * (повторный анализ того же стиха идемпотентен — дубли не создаются),
     * поэтому связь не перезаписывается целиком (§7 шаг 3).
     */
    private void attachBindings(UUID topicId, List<UUID> lexemeIds) {
        List<LexicalTopicBinding> bindings = new ArrayList<>();
        for (UUID lexemeId : lexemeIds) {
            LexicalTopicBindingId key = new LexicalTopicBindingId();
            key.setLexicalTopicId(topicId);
            key.setLexemeId(lexemeId);
            if (bindingRepository.existsById(key)) {
                continue;
            }
            LexicalTopicBinding binding = new LexicalTopicBinding();
            binding.setId(key);
            bindings.add(binding);
        }
        if (!bindings.isEmpty()) {
            bindingRepository.saveAll(bindings);
        }
    }

    /**
     * Перегенерация квест-единиц VERSE-урока после накопления привязок пачки.
     * Удаляет старые VOCABULARY_WORD единицы топика и собирает заново из всех
     * привязанных лексем (накопление по стихам главы → идемпотентно).
     */
    private void regenerateVerseTopicItems(Topic topic) {
        questItemRepository.deleteByTopicIdAndItemType(topic.getId(),
                VocabularyQuestItemTypes.VOCABULARY_WORD.code());
        lexicalQuizItemGenerator.generate(topic);
    }
    private static String emptyIfNull(String value) {
        return value == null ? "" : value;
    }

    private static String blankTo(String value, String fallback) {
        return LexiconImportService.isBlank(value) ? (fallback == null ? "" : fallback) : value;
    }
}