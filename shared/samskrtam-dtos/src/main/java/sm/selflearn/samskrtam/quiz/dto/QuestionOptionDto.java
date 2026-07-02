package sm.selflearn.samskrtam.quiz.dto;

import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder
public class QuestionOptionDto {
    UUID id;
    String formIast;
    String formDevanagari;
}
