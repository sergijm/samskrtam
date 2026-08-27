package sm.selflearn.samskrtam.curriculum.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerbalEndingDto {
    private Integer id;
    private String ending;
    private String lemmaSuffix;
    private Boolean hasAugment;
    private String tenseMood;
    private String personNumber;
    private String pada;
    private String notes;
}