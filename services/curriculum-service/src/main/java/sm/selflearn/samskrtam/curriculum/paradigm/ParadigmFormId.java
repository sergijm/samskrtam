package sm.selflearn.samskrtam.curriculum.paradigm;

import lombok.Data;
import sm.selflearn.samskrtam.content.model.CaseType;
import sm.selflearn.samskrtam.content.model.NumberType;
import sm.selflearn.samskrtam.content.model.VowelType;

import java.io.Serializable;

@Data
public class ParadigmFormId implements Serializable {
    private String lemmaIast;
    private VowelType vowelType;
    private CaseType caseType;
    private NumberType numberType;
}
