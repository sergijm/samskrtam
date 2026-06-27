package sm.selflearn.samskrtam.quiz.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;
import sm.selflearn.samskrtam.content.dto.LessonItemResponse;
import sm.selflearn.samskrtam.quiz.dto.QuestionDto;
import sm.selflearn.samskrtam.quiz.dto.StartOrResumeResponse;
import sm.selflearn.samskrtam.quiz.model.QuizSession;
import sm.selflearn.samskrtam.quiz.service.ContentClient;
import sm.selflearn.samskrtam.quiz.service.DeclensionOptionGeneratorService;
import sm.selflearn.samskrtam.quiz.service.LexicalOptionGeneratorService;

import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring")
public abstract class QuizSessionMapper {

    @Autowired
    protected ContentClient contentClient;

    @Autowired
    protected DeclensionOptionGeneratorService declensionOptionGeneratorService;

    @Autowired
    protected LexicalOptionGeneratorService lexicalOptionGeneratorService;

    @Mapping(target = "sessionId", source = "session.id")
    @Mapping(target = "lessonId", source = "session.lessonId")
    @Mapping(target = "lessonType", source = "session.lessonType")
    @Mapping(target = "totalQuestions", source = "session.totalQuestions")
    @Mapping(target = "answeredQuestions", expression = "java(answeredQuestionIds.size())")
    @Mapping(target = "score", source = "session.score")
    @Mapping(target = "currentQuestionIndex", expression = "java(answeredQuestionIds.size())")
    @Mapping(target = "currentQuestionNumber", expression = "java(answeredQuestionIds.size() + 1)")
    @Mapping(target = "lessonTitleRu", source = "lessonItem.titleRu")
    @Mapping(target = "lessonTitleEn", source = "lessonItem.titleEn")
    @Mapping(target = "lessonDescriptionRu", source = "lessonItem.descriptionRu")
    @Mapping(target = "lessonDescriptionEn", source = "lessonItem.descriptionEn")
    @Mapping(target = "slug", source = "lessonItem.slug")
    public abstract StartOrResumeResponse toStartOrResumeResponse(QuizSession session,
                                                                  List<QuestionDto> questions,
                                                                  LessonItemResponse lessonItem,
                                                                  List<UUID> answeredQuestionIds);
}

