package sm.selflearn.samskrtam.sangraha.dto;

import sm.selflearn.samskrtam.content.model.CaseType;
import sm.selflearn.samskrtam.content.model.NumberType;

import java.util.List;
import java.util.UUID;

public record DeclensionExamplesSearchResponseDto(
        List<GroupDto> groups
) {
    public record GroupDto(
            CaseType caseType,
            NumberType numberType,
            List<UUID> verseIds
    ) {}
}
