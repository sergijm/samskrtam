package sm.selflearn.samskrtam.quiz.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import sm.selflearn.samskrtam.quiz.dto.QuizSessionSummaryDto;
import sm.selflearn.samskrtam.quiz.model.QuizSession;

@Mapper(componentModel = "spring")
public interface UserSessionMapper {

    @Mapping(target = "sessionId", source = "id")
    QuizSessionSummaryDto toQuizSessionSummaryDto(QuizSession session);
}
