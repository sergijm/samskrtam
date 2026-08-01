package sm.selflearn.samskrtam.sangraha.dto;

import java.util.List;
import java.util.UUID;

public record VersesBatchRequestDto(
        List<UUID> verseIds
) {}
