package sm.selflearn.samskrtam.curriculum.dto;

import java.util.List;

/**
 * Payload for the learning map page: layers of topics with per-user progress.
 * Grouped by LearningLevel (L0..L6) plus one always-available evergreen layer.
 */
public record LearnGraphResponse(
        List<LearnLayerDto> layers
) {
}