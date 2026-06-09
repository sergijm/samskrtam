package sm.selflearn.samskrtam.quiz.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.UUID;

@Value
@Builder
public class QuestionDto {
    UUID id;
    String text;
    List<QuestionOptionDto> options;
}
