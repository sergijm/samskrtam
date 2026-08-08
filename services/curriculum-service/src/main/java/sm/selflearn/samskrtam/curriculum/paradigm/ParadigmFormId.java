package sm.selflearn.samskrtam.curriculum.paradigm;

import lombok.Data;
import sm.selflearn.samskrtam.content.model.CaseType;
import sm.selflearn.samskrtam.content.model.NumberType;

import java.io.Serializable;
import java.util.UUID;

@Data
public class ParadigmFormId implements Serializable {
    private UUID declensionStemId;
    private CaseType caseType;
    private NumberType numberType;
}