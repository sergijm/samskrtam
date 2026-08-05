package sm.selflearn.samskrtam.curriculum.dto;

import java.util.List;

public record TopicDetailDto(
        TopicDto topic,
        List<TopicPrerequisiteDto> prerequisites
) {
}
