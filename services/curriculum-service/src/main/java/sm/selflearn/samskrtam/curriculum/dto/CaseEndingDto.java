package sm.selflearn.samskrtam.curriculum.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import sm.selflearn.samskrtam.curriculum.lexicon.lingua.PosEnum;
import sm.selflearn.samskrtam.curriculum.lexicon.lingua.StemTypeEnum;
import sm.selflearn.samskrtam.curriculum.lexicon.model.LexemeGender;
import sm.selflearn.samskrtam.curriculum.questgen.morphology.CaseType;
import sm.selflearn.samskrtam.morphology.NumberType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CaseEndingDto {
    private Integer id;
    private StemTypeEnum stemType;
    private PosEnum pos;
    private LexemeGender gender;
    private NumberType number;
    private CaseType grammaticalCase;
    private String caseEnding;
}
