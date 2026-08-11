package sm.selflearn.samskrtam.quiz.service;

import org.springframework.stereotype.Component;
import sm.selflearn.samskrtam.quiz.model.QuizSession;
import sm.selflearn.samskrtam.quiz.model.SessionStatus;

import java.time.Instant;
import java.util.UUID;

@Component
public class SessionFactory {

    public QuizSession createComposedSession(UUID userId, int totalQuestions) {
        return QuizSession.builder()
                .id(null)
                .userId(userId)
                .lessonId(null)
                .lessonType(null)
                .totalQuestions(totalQuestions)
                .answeredQuestions(0)
                .score(0)
                .status(SessionStatus.IN_PROGRESS)
                .startedAt(Instant.now())
                .vocabularyWordsJson(null)
                .build();
    }
}