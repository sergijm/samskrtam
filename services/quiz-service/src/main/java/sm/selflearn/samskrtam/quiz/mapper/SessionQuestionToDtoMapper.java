package sm.selflearn.samskrtam.quiz.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import sm.selflearn.samskrtam.content.dto.GeneratedQuizQuestionDto;
import sm.selflearn.samskrtam.quiz.model.SessionQuestion;

@Mapper(componentModel = "spring")
public interface SessionQuestionToDtoMapper {

    @Mapping(target = "id", source = "questionId")
    @Mapping(target = "quizId", ignore = true)
    @Mapping(target = "questionNumber", source = "questionNumber")
    @Mapping(target = "text", source = "text")
    @Mapping(target = "explanationRu", source = "explanationRu")
    @Mapping(target = "explanationEn", source = "explanationEn")
    @Mapping(target = "declensionStemId", source = "declensionStemId")
    @Mapping(target = "targetCase", expression = "java(sessionQuestion.getTargetCase() != null ? sm.selflearn.samskrtam.content.model.CaseType.valueOf(sessionQuestion.getTargetCase()) : null)")
    @Mapping(target = "targetNumber", expression = "java(sessionQuestion.getTargetNumber() != null ? sm.selflearn.samskrtam.content.model.NumberType.valueOf(sessionQuestion.getTargetNumber()) : null)")
    @Mapping(target = "correctFormIast", source = "correctFormIast")
    @Mapping(target = "correctFormDevanagari", source = "correctFormDevanagari")
    @Mapping(target = "vocabularyWordId", source = "vocabularyWordId")
    @Mapping(target = "questionSourceLanguage", expression = "java(sessionQuestion.getQuestionSourceLanguage() != null ? sm.selflearn.samskrtam.content.dto.QuestionLanguage.valueOf(sessionQuestion.getQuestionSourceLanguage()) : null)")
    @Mapping(target = "questionTargetLanguage", expression = "java(sessionQuestion.getQuestionTargetLanguage() != null ? sm.selflearn.samskrtam.content.dto.QuestionLanguage.valueOf(sessionQuestion.getQuestionTargetLanguage()) : null)")
    @Mapping(target = "correctTranslationRu", source = "correctTranslationRu")
    @Mapping(target = "correctTranslationEn", source = "correctTranslationEn")
    @Mapping(target = "userLocale", ignore = true)
    @Mapping(target = "stem", source = "stem")
    @Mapping(target = "caseType", source = "targetCase")
    @Mapping(target = "numberType", source = "targetNumber")
    @Mapping(target = "stemDevanagari", source = "stemDevanagari")
    @Mapping(target = "stemTranslationRu", source = "stemTranslationRu")
    @Mapping(target = "stemTranslationEn", source = "stemTranslationEn")
    @Mapping(target = "gender", source = "targetGender")
    GeneratedQuizQuestionDto toDto(SessionQuestion sessionQuestion);
}
