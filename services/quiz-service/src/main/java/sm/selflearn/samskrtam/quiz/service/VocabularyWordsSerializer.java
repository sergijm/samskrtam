package sm.selflearn.samskrtam.quiz.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import sm.selflearn.samskrtam.common.SamskrtamException;
import sm.selflearn.samskrtam.content.dto.VocabularyWordDto;

import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class VocabularyWordsSerializer {

    private final ObjectMapper objectMapper;

    public String serialize(List<VocabularyWordDto> words) {
        if (words == null || words.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(words);
        } catch (JsonProcessingException e) {
            throw new SamskrtamException("JSON_PROCESSING_ERROR", "Failed to serialize vocabulary words", e);
        }
    }

    public List<VocabularyWordDto> deserialize(String json) {
        if (json == null) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, VocabularyWordDto.class));
        } catch (JsonProcessingException e) {
            throw new SamskrtamException("JSON_PROCESSING_ERROR", "Failed to deserialize vocabulary words", e);
        }
    }
}