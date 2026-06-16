package sm.selflearn.samskrtam.content.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class EamenauExerciseDetailDto {
    private Integer id;
    private Integer exerciseNumber;
    private String exerciseLetter;
    private String instructionText;
    private List<EamenauTaskDto> tasks;
}
