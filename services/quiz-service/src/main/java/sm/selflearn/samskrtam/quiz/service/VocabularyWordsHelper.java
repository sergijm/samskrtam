package sm.selflearn.samskrtam.quiz.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import sm.selflearn.samskrtam.common.SamskrtamException;
import sm.selflearn.samskrtam.content.dto.VocabularyWordDto;
import sm.selflearn.samskrtam.quiz.model.QuizSession;

import java.util.Collections;
import java.util.List;

/**
 * Helper for deserializing vocabulary words from QuizSession JSON.
 */
@Component
@RequiredArgsConstructor
public class VocabularyWordsHelper {

    private final VocabularyWordsSerializer vocabularyWordsSerializer;

    public Mono<List<VocabularyWordDto>> getVocabularyWords(QuizSession session) {
        if (session.getVocabularyWordsJson() == null) {
            return Mono.just(Collections.emptyList());
        }
        try {
            return Mono.just(vocabularyWordsSerializer.deserialize(session.getVocabularyWordsJson()));
        } catch (Exception e) {
            return Mono.error(new SamskrtamException("JSON_PROCESSING_ERROR", "Failed to deserialize vocabulary words", e));
        }
    }
}