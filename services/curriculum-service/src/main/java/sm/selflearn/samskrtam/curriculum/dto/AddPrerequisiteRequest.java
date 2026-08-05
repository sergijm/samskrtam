package sm.selflearn.samskrtam.curriculum.dto;

import jakarta.validation.constraints.NotNull;
import sm.selflearn.samskrtam.curriculum.model.PrerequisiteStrength;

import java.util.UUID;

public record AddPrerequisiteRequest(
        @NotNull
        UUID prerequisiteTopicId,

        @NotNull
        PrerequisiteStrength strength
) {
}
