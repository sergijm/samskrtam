package sm.selflearn.samskrtam.curriculum.questgen;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sm.selflearn.samskrtam.curriculum.lexicon.model.Lexeme;
import sm.selflearn.samskrtam.curriculum.lexicon.model.SemanticTopic;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.LexemeRepository;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.SemanticTopicRepository;
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
public class LexicalQuizItemGenerator extends QuizItemGenerator {

    public static final String GENERATOR_SOURCE = "LEXICAL_BATCH";

    static final int DISTRACTOR_COUNT = 3;

    private static final Random RANDOM = new Random();

    private final TopicRepository topicRepository;
    private final LexemeRepository lexemeRepository;
    private final QuestItemRepository questItemRepository;
    private final SemanticTopicRepository semanticTopicRepository;
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
        return domain == TopicDomain.LEXICON;
    }

    @Override
    public void ensureTopicsExist() {
        List<SemanticTopic> leaves = semanticTopicRepository.findAll().stream()
                .filter(st -> st.getParent() != null)
                .toList();
        for (SemanticTopic leaf : leaves) {
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
            topic.setSemanticTopicId(leaf.getId());
            topic.setEvergreen(false);
            topicRepository.save(topic);
        }
    }

    @Override
    @Transactional
    public int generate(Topic topic) {
        UUID semanticTopicId = topic.getSemanticTopicId();
        if (semanticTopicId == null) {
            return 0;
        }
        List<Lexeme> lexemes = lexemeRepository.findBySemanticTopics_Id(semanticTopicId);
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
        item.setAnswerMode(VocabularyQuestItemTypes.VOCABULARY_WORD.defaultAnswerMode().name());
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