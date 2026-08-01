package sm.selflearn.samskrtam.content.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.UUID;

/**
 * DTO для вызова POST {SANGRAHA_SERVICE_URL}/sangraha/internal/content/verses/batch
 * (sangraha-service.md §9). Клиентская копия sangraha DTO — не shared, т.к. content-service
 * не наследует sangraha-model.
 */
@Value
@Builder
public class SangrahaVersesBatchRequestDto {

    List<UUID> verseIds;

    @JsonCreator
    public SangrahaVersesBatchRequestDto(@JsonProperty("verseIds") List<UUID> verseIds) {
        this.verseIds = verseIds;
    }
}