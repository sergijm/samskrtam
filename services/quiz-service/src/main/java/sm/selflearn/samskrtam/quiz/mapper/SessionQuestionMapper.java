package sm.selflearn.samskrtam.quiz.mapper;

import org.springframework.stereotype.Component;
import sm.selflearn.samskrtam.content.dto.GeneratedQuizQuestionDto;
import sm.selflearn.samskrtam.quiz.model.SessionQuestion;

import java.util.UUID;

@Component
public class SessionQuestionMapper {

    public SessionQuestion fromDto(GeneratedQuizQuestionDto dto, UUID sessionId) {
        return SessionQuestion.builder()
                .sessionId(sessionId)
                .questionId(dto.getId())
                .questionNumber(dto.getQuestionNumber())
                .text(dto.getText())
                .explanationRu(dto.getExplanationRu())
                .explanationEn(dto.getExplanationEn())
                .declensionStemId(dto.getDeclensionStemId())
                .targetCase(dto.getTargetCase() != null ? dto.getTargetCase().name() : null)
                .targetNumber(dto.getTargetNumber() != null ? dto.getTargetNumber().name() : null)
                .correctFormIast(dto.getCorrectFormIast())
                .correctFormDevanagari(dto.getCorrectFormDevanagari())
                .vocabularyWordId(dto.getVocabularyWordId())
                .questionSourceLanguage(dto.getQuestionSourceLanguage() != null ? dto.getQuestionSourceLanguage().name() : null)
                .questionTargetLanguage(dto.getQuestionTargetLanguage() != null ? dto.getQuestionTargetLanguage().name() : null)
                .correctTranslationRu(dto.getCorrectTranslationRu())
                .correctTranslationEn(dto.getCorrectTranslationEn())
                .build();
    }
}