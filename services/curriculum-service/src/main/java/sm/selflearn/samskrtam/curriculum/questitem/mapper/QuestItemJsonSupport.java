package sm.selflearn.samskrtam.curriculum.questitem.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;
import sm.selflearn.samskrtam.curriculum.questitem.QuestItem;
import sm.selflearn.samskrtam.quest.AnswerMode;

import java.util.List;

/**
 * Helper for {@link QuestItemMapper}: deserializes the JSONB columns of
 * {@link QuestItem} and applies the MATCHING rule (no correct answer is exposed
 * to the client, see curriculum-quest-items.md §2.4 / §7).
 */
@Component
public class QuestItemJsonSupport {

    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    public QuestItemJsonSupport(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Named("jsonList")
    public List<String> toList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, STRING_LIST);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid distractors JSON: " + json, e);
        }
    }

    @Named("jsonObject")
    public Object toObject(String json) {
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid payload JSON: " + json, e);
        }
    }

    /** Exposes no correct answer for MATCHING items — verification happens on the backend. */
    public String correctAnswer(QuestItem item) {
        return item.getAnswerMode() == AnswerMode.MATCHING ? null : item.getCorrectAnswer();
    }

    /** Same MATCHING rule as {@link #correctAnswer} applied to the Russian variant. */
    public String correctAnswerRu(QuestItem item) {
        return item.getAnswerMode() == AnswerMode.MATCHING ? null : item.getCorrectAnswerRu();
    }
}
