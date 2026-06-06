package sm.selflearn.samskrtam.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnswerData {
    private UUID questionId;
    private UUID selectedOptionId;
    private String correctFormIast;
    private boolean correct;
    private int responseTimeMs;
    private Instant answeredAt;
}
