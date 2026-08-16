package sm.selflearn.samskrtam.curriculum.questgen;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sm.selflearn.samskrtam.curriculum.model.LearningLevel;
import sm.selflearn.samskrtam.curriculum.model.Topic;
import sm.selflearn.samskrtam.curriculum.model.TopicDomain;
import sm.selflearn.samskrtam.curriculum.model.TopicDomainType;
import sm.selflearn.samskrtam.curriculum.questitem.QuestItem;
import sm.selflearn.samskrtam.curriculum.questitem.repository.QuestItemRepository;
import sm.selflearn.samskrtam.curriculum.repository.TopicRepository;
import sm.selflearn.samskrtam.quest.SyntaxQuestItemTypes;
import sm.selflearn.samskrtam.quest.syntax.CaseMeaningPayload;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CaseMeaningQuizItemGenerator extends QuizItemGenerator {

    public static final String GENERATOR_SOURCE = "CASE_MEANING_BATCH";
    private static final String TOPIC_CODE = "case-meanings-basic";
    private static final String RESOURCE_PATH = "questdata/case-meanings.json";

    private static final Map<String, String> RU_CASE_MAP = ruCaseMap();

    private final TopicRepository topicRepository;
    private final QuestItemRepository questItemRepository;
    private final ObjectMapper objectMapper;

    private static Map<String, String> ruCaseMap() {
        Map<String, String> map = new LinkedHashMap<>();
        for (var ct : sm.selflearn.samskrtam.content.model.CaseType.values()) {
            map.put(ct.getRuName(), ct.name());
        }
        return map;
    }

    @Override
    public boolean isDomainSupported(TopicDomain domain) {
        return domain == TopicDomain.CASE_SYNTAX;
    }

    @Override
    public void ensureTopicsExist() {
        if (topicRepository.existsByCode(TOPIC_CODE)) {
            return;
        }
        Topic topic = new Topic();
        topic.setCode(TOPIC_CODE);
        topic.setTitleRu("Основные значения падежей");
        topic.setTitleEn("Basic case meanings");
        topic.setLearningLevel(LearningLevel.L1);
        topic.setDomain(TopicDomain.CASE_SYNTAX);
        topic.setDomainType(TopicDomainType.GRAMMAR);
        topic.setEvergreen(false);
        topicRepository.save(topic);
        log.info("Created topic {}", TOPIC_CODE);
    }

    @Override
    @Transactional
    public int generate(Topic topic) {
        if (!TOPIC_CODE.equals(topic.getCode())) {
            return 0;
        }
        List<QuestItem> items = loadCards(topic);
        if (items.isEmpty()) {
            return 0;
        }
        int saved = questItemRepository.saveAll(items).size();
        log.info("Generated {} CASE_MEANING quest items for topic {}", saved, TOPIC_CODE);
        return saved;
    }

    private List<QuestItem> loadCards(Topic topic) {
        JsonNode root;
        try (InputStream is = new ClassPathResource(RESOURCE_PATH).getInputStream()) {
            root = objectMapper.readTree(is);
        } catch (IOException e) {
            log.error("Failed to load case meanings from {}", RESOURCE_PATH, e);
            return List.of();
        }

        List<QuestItem> items = new LinkedList<>();

        JsonNode levels = root.get("levels");
        if (levels != null) {
            levels.fields().forEachRemaining(e ->
                    e.getValue().get("cards").forEach(card ->
                            items.add(buildItem(topic, card))));
        }

        JsonNode boss = root.get("boss_round");
        if (boss != null && boss.has("cards")) {
            boss.get("cards").forEach(card -> items.add(buildItem(topic, card)));
        }

        JsonNode meta = root.get("metadata");
        if (meta != null) {
            JsonNode dup = meta.get("duplicate_levels");
            if (dup != null) {
                dup.fields().forEachRemaining(e ->
                        e.getValue().get("cards").forEach(card ->
                                items.add(buildItem(topic, card))));
            }
        }

        return items;
    }

    private QuestItem buildItem(Topic topic, JsonNode card) {
        String question = card.get("question").asText();
        String correctAnswer = card.get("correct_answer").asText();

        List<String> options = new ArrayList<>();
        card.get("options").forEach(o -> options.add(o.asText()));

        List<String> distractors = new ArrayList<>(options);
        distractors.remove(correctAnswer);

        String cardType = jsonField(card, "type");
        String caseTypeRaw = jsonField(card, "case");
        String cardId = jsonField(card, "id");
        String sanskritExample = jsonField(card, "sanskrit_example");
        String transliteration = jsonField(card, "transliteration");
        String translation = jsonField(card, "translation");
        String explanation = jsonField(card, "explanation");

        String caseType = caseTypeRaw != null
                ? caseTypeRaw.toUpperCase()
                : RU_CASE_MAP.get(correctAnswer);

        String progressTag = caseType != null ? caseType : "UNKNOWN";

        CaseMeaningPayload payload = new CaseMeaningPayload(
                cardId, caseType, cardType,
                sanskritExample, transliteration, translation, explanation);

        QuestItem item = new QuestItem();
        item.setTopicId(topic.getId());
        item.setItemType(SyntaxQuestItemTypes.CASE_MEANING.code());
        item.setAnswerMode(SyntaxQuestItemTypes.CASE_MEANING.defaultAnswerMode().name());
        item.setPrompt(question);
        item.setPromptRu(question);
        item.setCorrectAnswer(correctAnswer);
        item.setCorrectAnswerRu(correctAnswer);
        item.setDistractors(toJson(distractors));
        item.setDistractorsRu(toJson(distractors));
        item.setPayload(toJson(payload));
        item.setProgressTag(progressTag);
        item.setGeneratorSource(GENERATOR_SOURCE);
        return item;
    }

    private static String jsonField(JsonNode node, String name) {
        JsonNode field = node.get(name);
        return field != null && !field.isNull() ? field.asText() : null;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to serialize quest item content", e);
        }
    }
}