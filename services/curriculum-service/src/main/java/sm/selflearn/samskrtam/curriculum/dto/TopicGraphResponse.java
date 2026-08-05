package sm.selflearn.samskrtam.curriculum.dto;

import java.util.List;

public record TopicGraphResponse(
        List<GraphLayerDto> layers,
        List<TopicDto> evergreen
) {
}
