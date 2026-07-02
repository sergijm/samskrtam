package sm.selflearn.samskrtam.quiz.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import sm.selflearn.samskrtam.content.model.Gender;
import sm.selflearn.samskrtam.content.model.VowelType;

/**
 * DTO для получения caseEndings из content-service.
 * Поля caseType/numberType — строки, т.к. content-service использует CaseType/NumberType
 * из своего пакета, а quiz-service использует Case/Number из shared.
 * Маппинг строк в enum-ы происходит через valueOf на стороне потребителя.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaseEndingDto {
    private Long id;
    private VowelType vowelType;
    private Gender gender;
    private String caseType;
    private String numberType;
    private String endingIast;
    private String endingDevanagari;
}