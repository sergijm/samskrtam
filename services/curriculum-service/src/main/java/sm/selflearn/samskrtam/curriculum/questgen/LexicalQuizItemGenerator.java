package sm.selflearn.samskrtam.curriculum.questgen;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sm.selflearn.samskrtam.curriculum.lexicon.imports.LexiconImportService;
import sm.selflearn.samskrtam.curriculum.lexicon.model.FrequencyBand;
import sm.selflearn.samskrtam.curriculum.lexicon.model.Lexeme;
import sm.selflearn.samskrtam.curriculum.lexicon.model.LexemeLexicalTopic;
import sm.selflearn.samskrtam.curriculum.lexicon.model.LexemeLexicalTopicId;
import sm.selflearn.samskrtam.curriculum.lexicon.model.PartOfSpeech;
import sm.selflearn.samskrtam.curriculum.lexicon.model.SemanticClass;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.FrequencyBandRepository;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.LexemeFrequencyRepository;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.LexemeRepository;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.LexemeLexicalTopicRepository;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.PartOfSpeechRepository;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.SemanticClassRepository;
import sm.selflearn.samskrtam.curriculum.model.LearningLevel;
import sm.selflearn.samskrtam.curriculum.model.Topic;
import sm.selflearn.samskrtam.curriculum.model.TopicDomain;
import sm.selflearn.samskrtam.curriculum.model.TopicDomainType;
import sm.selflearn.samskrtam.curriculum.questitem.QuestItem;
import sm.selflearn.samskrtam.curriculum.questitem.repository.QuestItemRepository;
import sm.selflearn.samskrtam.curriculum.repository.TopicRepository;
import sm.selflearn.samskrtam.quest.QuestItemType;
import sm.selflearn.samskrtam.quest.VocabularyQuestItemTypes;
import sm.selflearn.samskrtam.quest.lexicon.VocabularyWordPayload;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LexicalQuizItemGenerator extends QuizItemGenerator {

    public static final String GENERATOR_SOURCE = "LEXICAL_BATCH";

    /** Code prefix of the five frequency-band lessons (lexical-curriculum.md §2). */
    static final String FREQUENCY_TOPIC_PREFIX = "lex-frequency-";

    /** Code of the cumulative "500 most frequent words" lesson (§2, V31). */
    static final String TOP500_TOPIC_CODE = "lex-frequency-top500";

    /** Cumulative rank ceiling of the top-500 lesson (CORE + ESSENTIAL + FOUNDATIONAL). */
    static final int TOP500_MAX_RANK = 500;

    static final int DISTRACTOR_COUNT = 3;

    /** Code prefix of the part-of-speech lessons (V32). */
    static final String POS_TOPIC_PREFIX = "lex-pos-";

    private static final Random RANDOM = new Random();

    private final TopicRepository topicRepository;
    private final LexemeRepository lexemeRepository;
    private final QuestItemRepository questItemRepository;
    private final SemanticClassRepository semanticClassRepository;
    private final LexemeLexicalTopicRepository lexemeLexicalTopicRepository;
    private final FrequencyBandRepository frequencyBandRepository;
    private final LexemeFrequencyRepository lexemeFrequencyRepository;
    private final PartOfSpeechRepository partOfSpeechRepository;
    private final ObjectMapper objectMapper;

    static final Map<String, LearningLevel> SEMANTIC_LEVEL = Map.ofEntries(
            Map.entry("family-kin", LearningLevel.L0),
            Map.entry("body-parts", LearningLevel.L0),
            Map.entry("physical-action", LearningLevel.L0),
            Map.entry("animals", LearningLevel.L1),
            Map.entry("plants-trees", LearningLevel.L1),
            Map.entry("landscape", LearningLevel.L1),
            Map.entry("water", LearningLevel.L1),
            Map.entry("food-drink", LearningLevel.L1),
            Map.entry("house-dwelling", LearningLevel.L1),
            Map.entry("garments", LearningLevel.L1),
            Map.entry("sky-weather", LearningLevel.L2),
            Map.entry("professions", LearningLevel.L2),
            Map.entry("travel-vehicles", LearningLevel.L2),
            Map.entry("tools-materials", LearningLevel.L2),
            Map.entry("motion-verbs", LearningLevel.L2),
            Map.entry("speech-acts", LearningLevel.L2),
            Map.entry("senses", LearningLevel.L2),
            Map.entry("time-seasons", LearningLevel.L2),
            Map.entry("social-relations", LearningLevel.L3),
            Map.entry("emotions-positive", LearningLevel.L3),
            Map.entry("emotions-negative", LearningLevel.L3),
            Map.entry("desire-will", LearningLevel.L3),
            Map.entry("quantity-number", LearningLevel.L3),
            Map.entry("space-direction", LearningLevel.L3),
            Map.entry("rest-stillness", LearningLevel.L4),
            Map.entry("naming-address", LearningLevel.L4),
            Map.entry("question-answer", LearningLevel.L4),
            Map.entry("thought-memory", LearningLevel.L4),
            Map.entry("learning", LearningLevel.L4),
            Map.entry("ritual-worship", LearningLevel.L4),
            Map.entry("law-rule", LearningLevel.L4),
            Map.entry("phi-moksha", LearningLevel.L4),
            Map.entry("war-conflict", LearningLevel.L5));


    @Override
    public boolean isDomainSupported(TopicDomain domain) {
        return domain == TopicDomain.LEXICON || domain == TopicDomain.VERSE;
    }

    @Override
    @Transactional
    public void ensureTopicsExist() {
        List<SemanticClass> leaves = semanticClassRepository.findAll().stream()
                .filter(st -> st.getParent() != null)
                .toList();
        for (SemanticClass leaf : leaves) {
            if (topicRepository.findByCode(leaf.getCode()).isPresent()) {
                continue;
            }
            Topic topic = new Topic();
            topic.setCode(leaf.getCode());
            topic.setTitleRu(leaf.getNameRu());
            topic.setTitleEn(leaf.getNameEn());
            topic.setLearningLevel(SEMANTIC_LEVEL.getOrDefault(leaf.getCode(), LearningLevel.L0));
            topic.setDomain(TopicDomain.LEXICON);
            topic.setDomainType(TopicDomainType.LEXICON);
            topic.setSemanticClasses(Set.of(leaf));
            topic.setEvergreen(false);
            topicRepository.save(topic);
        }
        rebindFrequencyLessons();
        rebindPosLessons();
    }

    /**
     * (Re)populates the frequency lessons ({@code lex-frequency-*} codes,
     * lexical-curriculum.md §2) with the lexemes whose SANGRAHA_CORPUS frequency
     * rank falls inside each band, plus the cumulative top-500 lesson. Bands
     * live in {@code frequency_band}, so ranges stay in sync with the dashboard
     * without code changes.
     */
    private void rebindFrequencyLessons() {
        for (FrequencyBand band : frequencyBandRepository.findAllByOrderBySortOrderAsc()) {
            String code = FREQUENCY_TOPIC_PREFIX + band.getCode().toLowerCase();
            Topic topic = topicRepository.findByCode(code)
                    .orElseGet(() -> createFrequencyTopic(band, code));
            rebind(topic, band.getMinRank(), band.getMaxRank());
        }
        rebindTop500Lesson();
    }

    /** «500 самых частотных слов» — кумулятивный обзор первой половины словаря. */
    private void rebindTop500Lesson() {
        Topic topic = topicRepository.findByCode(TOP500_TOPIC_CODE)
                .orElseGet(this::createTop500Topic);
        rebind(topic, 1, TOP500_MAX_RANK);
    }

    private void rebind(Topic topic, int minRank, int maxRank) {
        List<UUID> lexemeIds = lexemeFrequencyRepository.findLexemeIdsBySourceAndRankRange(
                LexiconImportService.FREQUENCY_SOURCE, minRank, maxRank);
        bindLexemesToTopic(topic, lexemeIds);
        log.info("Frequency lesson {} bound to {} lexemes (rank {}-{})",
                topic.getCode(), lexemeIds.size(), minRank, maxRank);
    }

    /** (Re)populates the {@code lex-pos-*} lesson bindings from {@code lexeme_pos}. */
    private void rebindPosLessons() {
        for (PartOfSpeech pos : partOfSpeechRepository.findAll()) {
            String code = POS_TOPIC_PREFIX + pos.getCode();
            Topic topic = topicRepository.findByCode(code)
                    .orElseGet(() -> createPosTopic(pos, code));
            List<Lexeme> lexemes = lexemeRepository.findByPartsOfSpeech_CodeIn(Set.of(pos.getCode()));
            List<UUID> lexemeIds = lexemes.stream().map(Lexeme::getId).toList();
            bindLexemesToTopic(topic, lexemeIds);
            log.info("POS lesson {} bound to {} lexemes", topic.getCode(), lexemeIds.size());
        }
    }

    /** Deletes stale bindings for the topic and inserts fresh ones. */
    private void bindLexemesToTopic(Topic topic, List<UUID> lexemeIds) {
        lexemeLexicalTopicRepository.deleteByIdLexicalTopicId(topic.getId());
        List<LexemeLexicalTopic> bindings = lexemeIds.stream().map(lexemeId -> {
            LexemeLexicalTopicId key = new LexemeLexicalTopicId();
            key.setLexicalTopicId(topic.getId());
            key.setLexemeId(lexemeId);
            LexemeLexicalTopic binding = new LexemeLexicalTopic();
            binding.setId(key);
            return binding;
        }).toList();
        lexemeLexicalTopicRepository.saveAll(bindings);
    }

    /** Fallback for environments where the V30 seed did not run (idempotent with the migration). */
    private Topic createFrequencyTopic(FrequencyBand band, String code) {
        Topic topic = new Topic();
        topic.setCode(code);
        topic.setTitleRu(band.getLabelRu() + " (" + band.getMinRank() + "–" + band.getMaxRank() + ")");
        topic.setTitleEn(band.getLabelEn() + " (" + band.getMinRank() + "–" + band.getMaxRank() + ")");
        topic.setLearningLevel(LearningLevel.values()[Math.max(0, band.getSortOrder() - 1)]);
        topic.setDomain(TopicDomain.LEXICON);
        topic.setDomainType(TopicDomainType.LEXICON);
        topic.setEvergreen(false);
        topic.setDisplayOrder(band.getSortOrder());
        return topicRepository.save(topic);
    }

    /** Fallback for environments where the V31 seed did not run (idempotent with the migration). */
    private Topic createTop500Topic() {
        Topic topic = new Topic();
        topic.setCode(TOP500_TOPIC_CODE);
        topic.setTitleRu("500 самых частотных слов");
        topic.setTitleEn("500 most frequent words");
        topic.setLearningLevel(null);
        topic.setEvergreen(true);
        topic.setDomain(TopicDomain.LEXICON);
        topic.setDomainType(TopicDomainType.LEXICON);
        return topicRepository.save(topic);
    }

    /** Fallback for environments where the V32 seed did not run (idempotent with the migration). */
    private Topic createPosTopic(PartOfSpeech pos, String code) {
        Topic topic = new Topic();
        topic.setCode(code);
        topic.setTitleRu(pos.getNameRu());
        topic.setTitleEn(pos.getNameEn());
        topic.setLearningLevel(null);
        topic.setEvergreen(true);
        topic.setDomain(TopicDomain.LEXICON);
        topic.setDomainType(TopicDomainType.LEXICON);
        return topicRepository.save(topic);
    }

    @Override
    @Transactional
    public int generate(Topic topic) {
        List<Lexeme> lexemes = resolveLexemes(topic);
        if (lexemes.size() < DISTRACTOR_COUNT + 1) {
            return 0;
        }

        List<Lexeme> glossed = lexemes.stream()
                .filter(LexicalQuizItemGenerator::isGlossed)
                .collect(Collectors.toList());
        if (glossed.size() < DISTRACTOR_COUNT + 1) {
            return 0;
        }

        List<QuestItem> items = new ArrayList<>(glossed.size());
        for (Lexeme lexeme : glossed) {
            List<String[]> distractors = distractors(glossed, lexeme);
            if (distractors.size() < DISTRACTOR_COUNT) {
                continue;
            }
            List<String> distractorsEn = new ArrayList<>(DISTRACTOR_COUNT);
            List<String> distractorsRu = new ArrayList<>(DISTRACTOR_COUNT);
            for (String[] pair : distractors) {
                distractorsEn.add(pair[0]);
                distractorsRu.add(pair[1]);
            }
            items.add(buildItem(topic, lexeme, distractorsEn, distractorsRu));
        }
        return persist(items);
    }

    /**
     * LEXICON-тема берёт лексемы из обоих источников (lexical-curriculum.md §1):
     * семантические классы темы ({@code semantic_class_topic} →
     * {@code lexeme_semantic_class}) плюс явные привязки {@code lexeme_lexical_topic}.
     * VERSE-тема — только {@code lexeme_lexical_topic} (пачка стихов главы, §7).
     */
    private List<Lexeme> resolveLexemes(Topic topic) {
        Set<UUID> lexemeIds = new LinkedHashSet<>();
        lexemeLexicalTopicRepository.findByIdLexicalTopicId(topic.getId()).stream()
                .map(binding -> binding.getId().getLexemeId())
                .forEach(lexemeIds::add);
        if (topic.getDomain() != TopicDomain.VERSE) {
            Set<UUID> semanticClassIds = topic.getSemanticClasses().stream()
                    .map(SemanticClass::getId)
                    .collect(Collectors.toSet());
            if (!semanticClassIds.isEmpty()) {
                lexemeIds.addAll(lexemeRepository.findLexemeIdsBySemanticClassIds(semanticClassIds));
            }
        }
        if (lexemeIds.isEmpty()) {
            return List.of();
        }
        return lexemeRepository.findWithDetailsByIdIn(lexemeIds);
    }

    private static boolean isGlossed(Lexeme lexeme) {
        return notBlank(lexeme.getGlossEn()) && notBlank(lexeme.getGlossRu());
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private List<String[]> distractors(List<Lexeme> glossed, Lexeme correct) {
        Set<GlossPair> seen = new LinkedHashSet<>();
        List<String[]> candidates = new ArrayList<>();
        for (Lexeme lexeme : glossed) {
            if (lexeme.getId().equals(correct.getId())) {
                continue;
            }
            String glossEn = lexeme.getGlossEn();
            String glossRu = lexeme.getGlossRu();
            if (glossEn.equals(correct.getGlossEn()) || glossRu.equals(correct.getGlossRu())) {
                continue;
            }
            if (!seen.add(new GlossPair(glossEn, glossRu))) {
                continue;
            }
            candidates.add(new String[]{glossEn, glossRu});
            if (candidates.size() == DISTRACTOR_COUNT) {
                break;
            }
        }
        Collections.shuffle(candidates, RANDOM);
        return candidates;
    }

    private record GlossPair(String en, String ru) {}

    private QuestItem buildItem(Topic topic, Lexeme lexeme,
                                List<String> distractorsEn, List<String> distractorsRu) {
        String lemmaDevanagari = lexeme.getLemmaDevanagari();
        VocabularyWordPayload payload = new VocabularyWordPayload(
                lexeme.getLemmaSlp1(),
                lexeme.getLemmaIast(),
                lemmaDevanagari,
                lexeme.getGlossEn(),
                lexeme.getGlossRu());

        QuestItem item = new QuestItem();
        item.setTopicId(topic.getId());
        item.setItemType(VocabularyQuestItemTypes.VOCABULARY_WORD.code());
        item.setAnswerMode(VocabularyQuestItemTypes.VOCABULARY_WORD.defaultAnswerMode());
        item.setPrompt("What does '" + lemmaDevanagari + "' mean?");
        item.setPromptRu("Что значит " + quoteRu(lemmaDevanagari) + "?");
        item.setCorrectAnswer(lexeme.getGlossEn());
        item.setCorrectAnswerRu(lexeme.getGlossRu());
        item.setDistractors(toJson(distractorsEn));
        item.setDistractorsRu(toJson(distractorsRu));
        item.setPayload(toJson(payload));
        item.setProgressTag(lexeme.getLemmaSlp1());
        item.setGeneratorSource(GENERATOR_SOURCE);
        return item;
    }

    private static String quoteRu(String value) {
        return "«" + value + "»";
    }

    private int persist(List<QuestItem> items) {
        if (items.isEmpty()) {
            return 0;
        }
        return questItemRepository.saveAll(items).size();
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize quest item payload", e);
        }
    }
}