package sm.selflearn.samskrtam.quiz.mapper;

import org.mapstruct.Mapper;

import java.time.Duration;

@Mapper(componentModel = "spring", imports = Duration.class)
public interface UserSessionMapper {

//    @Mapping(target = "sessionId", source = "session.id")
//    @Mapping(target = "lessonId", source = "session.lessonId")
//    @Mapping(target = "lessonType", source = "session.lessonType")
//    @Mapping(target = "score", source = "session.score")
//    @Mapping(target = "totalQuestions", source = "session.totalQuestions")
//    @Mapping(target = "status", source = "session.status")
//    @Mapping(target = "startedAt", source = "session.startedAt")
//    @Mapping(target = "completedAt", source = "session.completedAt")
//    @Mapping(target = "lessonTitle", source = "lessonSummary.titleRu")
//    @Mapping(target = "lessonTitleRu", source = "lessonSummary.titleRu")
//    @Mapping(target = "lessonTitleEn", source = "lessonSummary.titleEn")
//    @Mapping(target = "slug", source = "lessonSummary.slug")
//    @Mapping(target = "durationMs", expression = "java(session.getStartedAt() != null && session.getCompletedAt() != null ? Duration.between(session.getStartedAt(), session.getCompletedAt()).toMillis() : null)")
//    QuizSummaryDto toQuizSummaryDto(QuizSession session, LessonSummaryDto lessonSummary);
}

