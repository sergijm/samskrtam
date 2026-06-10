package sm.selflearn.samskrtam.content.dto;

import lombok.Builder;
import lombok.Value;
import sm.selflearn.samskrtam.content.model.Case;
import sm.selflearn.samskrtam.content.model.Number;

import java.util.UUID;

@Value
@Builder
public class GeneratedQuestionDto {
    UUID id;
    Integer questionNumber;
    String text;
    String explanationRu;
    String explanationEn;
    UUID declensionStemId;
    Case targetCase;
    Number targetNumber;
    String correctFormIast;
    String correctFormDevanagari;
    UUID vocabularyWordId;
    QuestionLanguage questionSourceLanguage;
    QuestionLanguage questionTargetLanguage;
    String correctTranslationRu;
    String correctTranslationEn;
}
