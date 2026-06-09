package sm.selflearn.samskrtam.quiz.dto;

import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder
public class CompleteSessionResponse {
    UUID sessionId;
    int score;
    int totalQuestions;
    long durationMs;
}
