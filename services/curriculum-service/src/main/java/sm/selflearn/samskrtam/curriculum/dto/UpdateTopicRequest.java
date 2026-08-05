package sm.selflearn.samskrtam.curriculum.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import sm.selflearn.samskrtam.curriculum.model.LearningLevel;

public record UpdateTopicRequest(
        @NotBlank
        @Size(max = 200)
        String titleRu,

        @NotBlank
        @Size(max = 200)
        String titleEn,

        @NotNull
        LearningLevel learningLevel,

        Boolean isEvergreen,
        Short displayOrder
) {
}
