package sm.selflearn.samskrtam.curriculum.dto;

import java.util.List;

public record GraphLayerDto(
        int layer,
        List<TopicDto> topics
) {
}
