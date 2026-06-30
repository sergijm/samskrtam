package sm.selflearn.samskrtam.quiz.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import sm.selflearn.samskrtam.content.dto.GeneratedQuizQuestionDto;
import sm.selflearn.samskrtam.quiz.model.SessionQuestion;

import java.util.UUID;

@Mapper(componentModel = "spring")
public interface SessionQuestionMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "sessionId", source = "sessionId")
    @Mapping(target = "questionId", source = "dto.id")
    @Mapping(target = "questionNumber", source = "dto.questionNumber")
    @Mapping(target = "text", source = "dto.text")
    @Mapping(target = "explanationRu", source = "dto.explanationRu")
    @Mapping(target = "explanationEn", source = "dto.explanationEn")
    @Mapping(target = "declensionStemId", source = "dto.declensionStemId")
    @Mapping(target = "targetCase", expression = "java(dto.getTargetCase() != null ? dto.getTargetCase().name() : null)")
    @Mapping(target = "targetNumber", expression = "java(dto.getTargetNumber() != null ? dto.getTargetNumber().name() : null)")
    @Mapping(target = "correctFormIast", source = "dto.correctFormIast")
    @Mapping(target = "correctFormDevanagari", source = "dto.correctFormDevanagari")
    @Mapping(target = "vocabularyWordId", source = "dto.vocabularyWordId")
    @Mapping(target = "questionSourceLanguage", expression = "java(dto.getQuestionSourceLanguage() != null ? dto.getQuestionSourceLanguage().name() : null)")
    @Mapping(target = "questionTargetLanguage", expression = "java(dto.getQuestionTargetLanguage() != null ? dto.getQuestionTargetLanguage().name() : null)")
    @Mapping(target = "correctTranslationRu", source = "dto.correctTranslationRu")
    @Mapping(target = "correctTranslationEn", source = "dto.correctTranslationEn")
    SessionQuestion fromDto(GeneratedQuizQuestionDto dto, UUID sessionId);
}
