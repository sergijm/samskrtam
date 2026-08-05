package sm.selflearn.samskrtam.curriculum.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import sm.selflearn.samskrtam.curriculum.model.ComplexQuizType;
import sm.selflearn.samskrtam.curriculum.model.LearningLevel;

import java.util.List;
import java.util.UUID;

public record UpsertComplexQuizRequest(
        @NotNull
        ComplexQuizType type,

        @NotNull
        LearningLevel learningLevel,

        @NotBlank
        @Size(max = 200)
        String titleRu,

        @NotBlank
        @Size(max = 200)
        String titleEn,

        Short questionCountHint,

        @NotNull
        @Size(min = 2, max = 7)
        List<@NotNull UUID> topicIds
) {
}
