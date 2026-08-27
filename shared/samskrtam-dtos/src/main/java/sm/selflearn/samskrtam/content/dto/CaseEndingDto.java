package sm.selflearn.samskrtam.content.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import sm.selflearn.samskrtam.content.model.CaseType;
import sm.selflearn.samskrtam.morphology.Gender;
import sm.selflearn.samskrtam.morphology.NumberType;
import sm.selflearn.samskrtam.content.model.VowelType;

import java.util.UUID;

/**
 * DTO для получения caseEndings из content-service.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaseEndingDto {
    private UUID id;
    private VowelType vowelType;
    private Gender gender;
    private CaseType caseType;
    private NumberType numberType;
    private String endingIast;
    private String endingDevanagari;
}