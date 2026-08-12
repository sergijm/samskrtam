package sm.selflearn.samskrtam.quiz.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import sm.selflearn.samskrtam.quiz.dto.AnswerHistoryEntry;
import sm.selflearn.samskrtam.quiz.dto.AnswerResponse;
import sm.selflearn.samskrtam.content.dto.GeneratedQuizQuestionDto;
import sm.selflearn.samskrtam.quiz.model.QuizAnswer;
import sm.selflearn.samskrtam.quiz.model.QuizSession;

import java.util.UUID;

@Mapper(componentModel = "spring")
public interface QuizAnswerMapper {

    @Mapping(target = "correct", source = "isCorrect")
    @Mapping(target = "correctOptionId", source = "correctWordId")
    @Mapping(target = "correctAnswerText", source = "correctAnswerText")
    @Mapping(target = "explanationRu", source = "generatedQuestion.explanationRu")
    @Mapping(target = "explanationEn", source = "generatedQuestion.explanationEn")
    @Mapping(target = "questionNumber", expression = "java(session.getAnsweredQuestions() + 1)")
    @Mapping(target = "totalQuestions", source = "session.totalQuestions")
    AnswerResponse toAnswerResponse(boolean isCorrect, UUID correctWordId, String correctAnswerText, GeneratedQuizQuestionDto generatedQuestion, QuizSession session);

    @Mapping(target = "answeredAt", expression = "java(qa.getAnsweredAt() != null ? java.time.LocalDateTime.ofInstant(qa.getAnsweredAt(), java.time.ZoneOffset.UTC) : null)")
    @Mapping(target = "correctAnswer", source = "qa.correctFormIast")
    @Mapping(target = "userAnswer", source = "qa.selectedFormIast")
    @Mapping(target = "correct", source = "qa.isCorrect")
    AnswerHistoryEntry toAnswerHistoryEntry(QuizAnswer qa);
}

