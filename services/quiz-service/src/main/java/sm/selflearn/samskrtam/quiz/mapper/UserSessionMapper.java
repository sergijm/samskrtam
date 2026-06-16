package sm.selflearn.samskrtam.quiz.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import sm.selflearn.samskrtam.content.dto.QuizSummaryDto;
import sm.selflearn.samskrtam.quiz.dto.QuizSessionSummaryDto;
import sm.selflearn.samskrtam.quiz.model.QuizSession;

import java.time.Duration; // Import Duration

@Mapper(componentModel = "spring", imports = Duration.class)
public interface UserSessionMapper {

    @Mapping(target = "sessionId", source = "session.id")
    @Mapping(target = "quizId", source = "session.quizId")
    @Mapping(target = "quizType", source = "session.quizType")
    @Mapping(target = "score", source = "session.score")
    @Mapping(target = "totalQuestions", source = "session.totalQuestions")
    @Mapping(target = "status", source = "session.status")
    @Mapping(target = "startedAt", source = "session.startedAt")
    @Mapping(target = "completedAt", source = "session.completedAt")
    @Mapping(target = "quizTitle", source = "quizSummary.titleRu") // Default to Russian title
    @Mapping(target = "quizTitleRu", source = "quizSummary.titleRu")
    @Mapping(target = "quizTitleEn", source = "quizSummary.titleEn")
    @Mapping(target = "slug", source = "quizSummary.slug")
    @Mapping(target = "durationMs", expression = "java(session.getStartedAt() != null && session.getCompletedAt() != null ? Duration.between(session.getStartedAt(), session.getCompletedAt()).toMillis() : null)")
    QuizSessionSummaryDto toQuizSessionSummaryDto(QuizSession session, QuizSummaryDto quizSummary);
}
