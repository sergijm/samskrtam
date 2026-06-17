package sm.selflearn.samskrtam.content.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SandhiRuleGroupDto {
    private Integer id;
    private String description;
    private String code;
}
