package sm.selflearn.samskrtam.quiz.dto;

import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder
public class AnswerRequest {
    UUID questionId;
    UUID selectedOptionId;
    int responseTimeMs;
}
