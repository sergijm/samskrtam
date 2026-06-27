package sm.selflearn.samskrtam.content.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EmenauExerciseDto {
    private Integer id;
    private Integer exerciseNumber;
    private String exerciseLetter;
    private String instructionText;
}
