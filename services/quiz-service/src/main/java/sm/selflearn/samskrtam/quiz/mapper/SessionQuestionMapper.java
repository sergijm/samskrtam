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
    @Mapping(target = "stem", source = "dto.stem")
    @Mapping(target = "stemDevanagari", source = "dto.stemDevanagari")
    @Mapping(target = "stemTranslationRu", source = "dto.stemTranslationRu")
    @Mapping(target = "stemTranslationEn", source = "dto.stemTranslationEn")
    @Mapping(target = "targetCase", expression = "java(dto.getTargetCase() != null ? dto.getTargetCase().name() : null)")
    @Mapping(target = "targetNumber", expression = "java(dto.getTargetNumber() != null ? dto.getTargetNumber().name() : null)")
    @Mapping(target = "correctFormIast", source = "dto.correctFormIast")
    @Mapping(target = "correctFormDevanagari", source = "dto.correctFormDevanagari")
    @Mapping(target = "vocabularyWordId", source = "dto.vocabularyWordId")
    @Mapping(target = "caseEndingId", source = "dto.caseEndingId")
    @Mapping(target = "itemType", source = "dto.itemType")
    @Mapping(target = "questionSourceLanguage", expression = "java(dto.getQuestionSourceLanguage() != null ? dto.getQuestionSourceLanguage().name() : null)")
    @Mapping(target = "correctTranslationRu", source = "dto.correctTranslationRu")
    @Mapping(target = "correctTranslationEn", source = "dto.correctTranslationEn")
    SessionQuestion fromDto(GeneratedQuizQuestionDto dto, UUID sessionId);
}

