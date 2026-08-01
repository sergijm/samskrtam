package sm.selflearn.samskrtam.sangraha.dto;

import sm.selflearn.samskrtam.content.model.CaseType;
import sm.selflearn.samskrtam.content.model.Gender;
import sm.selflearn.samskrtam.content.model.NumberType;
import sm.selflearn.samskrtam.content.model.VowelType;

import java.util.List;

public record DeclensionExamplesSearchRequestDto(
        VowelType vowelType,
        Gender gender,
        int limitPerGroup,
        List<CellDto> cells
) {
    public record CellDto(CaseType caseType, NumberType numberType) {}
}
