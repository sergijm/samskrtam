package sm.selflearn.samskrtam.quiz.dto;

import java.util.UUID; // Import UUID

public record QuizProgressDto(
        UUID sessionId, // New field to store the session ID
        int answeredQuestions,
        int totalQuestions,
        boolean found
) {}
