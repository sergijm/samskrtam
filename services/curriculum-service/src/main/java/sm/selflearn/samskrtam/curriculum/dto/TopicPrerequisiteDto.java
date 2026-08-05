package sm.selflearn.samskrtam.curriculum.dto;

import sm.selflearn.samskrtam.curriculum.model.PrerequisiteStrength;

public record TopicPrerequisiteDto(
        TopicDto topic,
        PrerequisiteStrength strength
) {
}
