package sm.selflearn.samskrtam.quiz.dto;

import java.util.UUID;

public record QuizProgressDto(
        UUID sessionId,
        int answeredQuestions,
        int totalQuestions,
        boolean found
) {}
