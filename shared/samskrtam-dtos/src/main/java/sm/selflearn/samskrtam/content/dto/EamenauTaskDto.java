package sm.selflearn.samskrtam.content.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EamenauTaskDto {
    private Integer id;
    private Integer taskNumber;
    private String taskText;
    private SolutionDto solution;
}
