package sm.selflearn.samskrtam.curriculum.questgen;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sm.selflearn.samskrtam.curriculum.lexicon.model.FrequencyBand;
import sm.selflearn.samskrtam.curriculum.lexicon.model.PartOfSpeech;
import sm.selflearn.samskrtam.curriculum.lexicon.model.SemanticClass;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.FrequencyBandRepository;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.LemmaLexicalTopicRepository;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.LemmaTranslationRepository;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.PartOfSpeechRepository;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.SemanticClassRepository;
import sm.selflearn.samskrtam.curriculum.lexicon.model.LemmaTranslation;
import sm.selflearn.samskrtam.curriculum.lexicon.service.TransliterationService;
import sm.selflearn.samskrtam.curriculum.model.LearningLevel;
import sm.selflearn.samskrtam.curriculum.model.Topic;
import sm.selflearn.samskrtam.curriculum.model.TopicDomain;
import sm.selflearn.samskrtam.curriculum.model.TopicDomainType;
import sm.selflearn.samskrtam.curriculum.questitem.QuestItem;
import sm.selflearn.samskrtam.curriculum.questitem.repository.QuestItemRepository;
import sm.selflearn.samskrtam.curriculum.repository.TopicRepository;
import sm.selflearn.samskrtam.quest.QuestItemType;
import sm.selflearn.samskrtam.quest.QuestPatterns;
import sm.selflearn.samskrtam.quest.VocabularyQuestItemTypes;
import sm.selflearn.samskrtam.quest.HighlightToken;
import sm.selflearn.samskrtam.quest.lexicon.VocabularyWordPayload;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Generates VOCABULARY_WORD quest items for lexical topics straight from
 * {@code curriculum.lemma_translation} — no {@code Lexeme} dependency.
 *
 * <p>Candidates are unique {@code lemma_iast} spellings resolved per topic:
 * <ul>
 *   <li>semantic lessons — via {@code lemma_semantic_class} (topic → semantic class);</li>
 *   <li>frequency lessons ({@code lex-frequency-*}, {@code lex-frequency-top500}) —
 *       via {@code lingua.lemma_frequency} rank window intersected with the
 *       translated lemmas;</li>
 *   <li>part-of-speech lessons ({@code lex-pos-*}) — via the {@code pos} column
 *       of {@code lemma_translation} (Friš part-of-speech code).</li>
 * </ul>
 *
 * Every produced item carries a {@code progress_tag} equal to its {@code lemma_iast}
 * (the unique spelling), see V39.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LexicalQuizItemGenerator extends QuizItemGenerator {

    public static final String GENERATOR_SOURCE = "LEXICAL_BATCH";

    /** Code prefix of the frequency-band lessons (lexical-curriculum.md §2). */
    static final String FREQUENCY_TOPIC_PREFIX = "lex-frequency-";

    /** Code of the cumulative "500 most frequent words" lesson (§2, V31). */
    static final String TOP500_TOPIC_CODE = "lex-frequency-top500";

    /** Cumulative rank ceiling of the top-500 lesson (lingua.lemma_frequency.row_num). */
    static final int TOP500_MAX_RANK = 500;

    static final int DISTRACTOR_COUNT = 3;

    /** Code prefix of the part-of-speech lessons (V32). */
    static final String POS_TOPIC_PREFIX = "lex-pos-";

    /**
     * Maps a {@code lex-pos-*} topic suffix to the Friš {@code part_of_speech}
     * code stored in {@code lemma_translation.pos}. Suffixes without a Friš
     * equivalent (preverb, preposition) yield no candidates.
     */
    private static final Map<String, String> POS_SUFFIX_TO_FRISCH = Map.ofEntries(
            Map.entry("noun", "NOUN"),
            Map.entry("adjective", "ADJECTIVE"),
            Map.entry("pronoun", "PRONOUN"),
            Map.entry("numeral", "NUMERAL"),
            Map.entry("finite-verb", "VERB"),
            Map.entry("participle", "VERB"),
            Map.entry("infinitive", "VERB"),
            Map.entry("absolutive", "VERB"),
            Map.entry("gerund", "VERB"),
            Map.entry("adverb", "ADVERB"),
            Map.entry("particle", "PARTICLE"),
            Map.entry("conjunction", "CONJUNCTION"),
            Map.entry("interjection", "INTERJECTION"));

    private static final Random RANDOM = new Random();

    private final TopicRepository topicRepository;
    private final QuestItemRepository questItemRepository;
    private final SemanticClassRepository semanticClassRepository;
    private final FrequencyBandRepository frequencyBandRepository;
    private final PartOfSpeechRepository partOfSpeechRepository;
    private final LemmaTranslationRepository lemmaTranslationRepository;
    private final LemmaLexicalTopicRepository lemmaLexicalTopicRepository;
    private final TransliterationService transliterationService;
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
        // Semantic-class lessons come from the leaves of the semantic-class tree.
        for (SemanticClass leaf : semanticClassRepository.findAll().stream()
                .filter(st -> st.getParent() != null)
                .toList()) {
            if (topicRepository.findByCode(leaf.getCode()).isPresent()) {
                continue;
            }
            topicRepository.save(createSemanticTopic(leaf));
        }
        // Frequency lessons come from the seeded frequency bands + the cumulative top-500.
        for (FrequencyBand band : frequencyBandRepository.findAllByOrderBySortOrderAsc()) {
            String code = FREQUENCY_TOPIC_PREFIX + band.getCode().toLowerCase();
            topicRepository.findByCode(code)
                    .ifPresentOrElse(t -> {}, () -> topicRepository.save(createFrequencyTopic(band, code)));
        }
        topicRepository.findByCode(TOP500_TOPIC_CODE)
                .ifPresentOrElse(t -> {}, () -> topicRepository.save(createTop500Topic()));
        // Part-of-speech lessons come from the seeded part-of-speech codes.
        for (PartOfSpeech pos : partOfSpeechRepository.findAll()) {
            String code = POS_TOPIC_PREFIX + pos.getCode();
            topicRepository.findByCode(code)
                    .ifPresentOrElse(t -> {}, () -> topicRepository.save(createPosTopic(pos, code)));
        }
    }

    @Override
    @Transactional
    public int generate(Topic topic) {
        List<String> lemmaIasts = resolveLemmaIasts(topic);
        if (lemmaIasts.isEmpty()) {
            return 0;
        }

        Map<String, Translation> translations = loadTranslations(new LinkedHashSet<>(lemmaIasts));
        if (translations.size() < DISTRACTOR_COUNT + 1) {
            return 0;
        }

        List<Translation> pool = new ArrayList<>(translations.values());
        List<QuestItem> items = new ArrayList<>();
        for (Translation t : pool) {
            List<String[]> distractors = distractors(pool, t);
            if (distractors.size() < DISTRACTOR_COUNT) {
                continue;
            }
            List<String> distractorsEn = new ArrayList<>(DISTRACTOR_COUNT);
            List<String> distractorsRu = new ArrayList<>(DISTRACTOR_COUNT);
            for (String[] pair : distractors) {
                distractorsEn.add(pair[0]);
                distractorsRu.add(pair[1]);
            }
            items.add(buildItem(topic, t, distractorsEn, distractorsRu));
        }
        return persist(items);
    }

    /**
     * Resolves the unique {@code lemma_iast} spellings a topic quizzes, from
     * {@code lemma_translation} (no Lexeme involved).
     */
    private List<String> resolveLemmaIasts(Topic topic) {
        String code = topic.getCode();
        if (topic.getDomain() == TopicDomain.VERSE) {
            return lemmaLexicalTopicRepository.findDistinctLemmaIastByTopicCode(code);
        }
        if (code.startsWith(FREQUENCY_TOPIC_PREFIX)) {
            if (TOP500_TOPIC_CODE.equals(code)) {
                return lemmaTranslationRepository.findDistinctLemmaIastByFrequencyRankRange(1, TOP500_MAX_RANK);
            }
            String band = code.substring(FREQUENCY_TOPIC_PREFIX.length());
            return frequencyBandRepository.findByCode(band.toUpperCase())
                    .map(b -> lemmaTranslationRepository.findDistinctLemmaIastByFrequencyRankRange(
                            b.getMinRank(), b.getMaxRank()))
                    .orElse(List.of());
        }
        if (code.startsWith(POS_TOPIC_PREFIX)) {
            String frischPos = POS_SUFFIX_TO_FRISCH.get(code.substring(POS_TOPIC_PREFIX.length()));
            if (frischPos == null) {
                return List.of();
            }
            return lemmaTranslationRepository.findDistinctLemmaIastByPos(frischPos);
        }
        Set<UUID> classIds = topic.getSemanticClasses().stream()
                .map(SemanticClass::getId)
                .collect(Collectors.toSet());
        if (classIds.isEmpty()) {
            return List.of();
        }
        return lemmaTranslationRepository.findDistinctLemmaIastBySemanticClassIds(classIds);
    }

    /**
     * Loads the main ru/en gloss for each lemma and keeps only lemmas that have
     * both languages (a vocabulary item needs a meaning in both learner tongues).
     */
    private Map<String, Translation> loadTranslations(Set<String> lemmaIasts) {
        Map<String, Translation> byLemma = new LinkedHashMap<>();
        for (LemmaTranslation row : lemmaTranslationRepository.findByLemmaIastIn(lemmaIasts)) {
            Translation t = byLemma.computeIfAbsent(row.getLemmaIast(),
                    k -> new Translation(k, transliterationService.iastToDevanagari(k)));
            if (row.isMain()) {
                if ("en".equals(row.getLanguage())) {
                    t.glossEn = row.getGloss();
                } else if ("ru".equals(row.getLanguage())) {
                    t.glossRu = row.getGloss();
                }
            }
        }
        byLemma.values().removeIf(t -> t.glossEn == null || t.glossRu == null);
        return byLemma;
    }

    private List<String[]> distractors(List<Translation> pool, Translation correct) {
        Set<GlossPair> seen = new LinkedHashSet<>();
        List<String[]> candidates = new ArrayList<>();
        for (Translation other : pool) {
            if (other.lemmaIast.equals(correct.lemmaIast)) {
                continue;
            }
            if (other.glossEn.equals(correct.glossEn) || other.glossRu.equals(correct.glossRu)) {
                continue;
            }
            if (!seen.add(new GlossPair(other.glossEn, other.glossRu))) {
                continue;
            }
            candidates.add(new String[]{other.glossEn, other.glossRu});
            if (candidates.size() == DISTRACTOR_COUNT) {
                break;
            }
        }
        Collections.shuffle(candidates, RANDOM);
        return candidates;
    }

    private QuestItem buildItem(Topic topic, Translation t,
                                List<String> distractorsEn, List<String> distractorsRu) {
        List<HighlightToken> highlights = List.of(new HighlightToken(t.lemmaIast, t.lemmaIast));

        VocabularyWordPayload payload = new VocabularyWordPayload(
                null,                       // lemmaSlp1 — not stored in lemma_translation
                t.lemmaIast,
                t.lemmaDevanagari,
                t.glossEn,
                t.glossRu,
                highlights);

        QuestItem item = new QuestItem();
        item.setTopicId(topic.getId());
        item.setItemType(VocabularyQuestItemTypes.VOCABULARY_WORD.code());
        item.setAnswerMode(VocabularyQuestItemTypes.VOCABULARY_WORD.defaultAnswerMode());
        item.setPrompt("What does " + sanskritWord(t.lemmaIast, t.lemmaDevanagari) + " mean?");
        item.setPromptRu("Что значит " + sanskritWord(t.lemmaIast, t.lemmaDevanagari) + "?");
        item.setCorrectAnswer(t.glossEn);
        item.setCorrectAnswerRu(t.glossRu);
        item.setDistractors(toJson(distractorsEn));
        item.setDistractorsRu(toJson(distractorsRu));
        item.setPayload(toJson(payload));
        item.setProgressTag(t.lemmaIast);
        item.setQuestPattern(QuestPatterns.LEX_TRAN);
        item.setGeneratorSource(GENERATOR_SOURCE);
        return item;
    }

    private static String sanskritWord(String iast, String devanagari) {
        if (devanagari == null || devanagari.isBlank()) {
            return iast;
        }
        return iast + " (" + devanagari + ")";
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

    // ----------------------------------------------------------------------
    // Topic provisioning (no Lexeme bindings — resolution is computed on generate)
    // ----------------------------------------------------------------------

    private Topic createSemanticTopic(SemanticClass leaf) {
        Topic topic = new Topic();
        topic.setCode(leaf.getCode());
        topic.setTitleRu(leaf.getNameRu());
        topic.setTitleEn(leaf.getNameEn());
        topic.setLearningLevel(SEMANTIC_LEVEL.getOrDefault(leaf.getCode(), LearningLevel.L0));
        topic.setDomain(TopicDomain.LEXICON);
        topic.setDomainType(TopicDomainType.LEXICON);
        topic.setSemanticClasses(Set.of(leaf));
        topic.setEvergreen(false);
        return topic;
    }

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
        return topic;
    }

    private Topic createTop500Topic() {
        Topic topic = new Topic();
        topic.setCode(TOP500_TOPIC_CODE);
        topic.setTitleRu("500 самых частотных слов");
        topic.setTitleEn("500 most frequent words");
        topic.setLearningLevel(null);
        topic.setEvergreen(true);
        topic.setDomain(TopicDomain.LEXICON);
        topic.setDomainType(TopicDomainType.LEXICON);
        return topic;
    }

    private Topic createPosTopic(PartOfSpeech pos, String code) {
        Topic topic = new Topic();
        topic.setCode(code);
        topic.setTitleRu(pos.getNameRu());
        topic.setTitleEn(pos.getNameEn());
        topic.setLearningLevel(null);
        topic.setEvergreen(true);
        topic.setDomain(TopicDomain.LEXICON);
        topic.setDomainType(TopicDomainType.LEXICON);
        return topic;
    }

    // ----------------------------------------------------------------------
    // Value holders
    // ----------------------------------------------------------------------

    private static final class Translation {
        final String lemmaIast;
        final String lemmaDevanagari;
        String glossEn;
        String glossRu;

        Translation(String lemmaIast, String lemmaDevanagari) {
            this.lemmaIast = lemmaIast;
            this.lemmaDevanagari = lemmaDevanagari;
        }
    }

    private record GlossPair(String en, String ru) {
    }
}
