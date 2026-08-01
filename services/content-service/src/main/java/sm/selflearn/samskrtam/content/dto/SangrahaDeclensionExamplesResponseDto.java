package sm.selflearn.samskrtam.content.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.UUID;

/**
 * Ответ от POST {SANGRAHA_SERVICE_URL}/sangraha/internal/content/declension-examples
 * (sangraha-service.md §9). Клиентская копия sangraha DTO.
 */
@Value
@Builder
public class SangrahaDeclensionExamplesResponseDto {

    List<GroupDto> groups;

    @JsonCreator
    public SangrahaDeclensionExamplesResponseDto(@JsonProperty("groups") List<GroupDto> groups) {
        this.groups = groups;
    }

    @Value
    @Builder
    public static class GroupDto {
        String caseType;
        String numberType;
        List<UUID> verseIds;

        @JsonCreator
        public GroupDto(
                @JsonProperty("caseType") String caseType,
                @JsonProperty("numberType") String numberType,
                @JsonProperty("verseIds") List<UUID> verseIds) {
            this.caseType = caseType;
            this.numberType = numberType;
            this.verseIds = verseIds;
        }
    }
}