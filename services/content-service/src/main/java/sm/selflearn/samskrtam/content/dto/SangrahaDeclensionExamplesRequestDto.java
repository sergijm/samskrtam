package sm.selflearn.samskrtam.content.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * DTO для вызова POST {SANGRAHA_SERVICE_URL}/sangraha/internal/content/declension-examples
 * (sangraha-service.md §9). Клиентская копия sangraha DTO.
 * <p>
 * sangraha-сервис использует свои enum (sm.selflearn.samskrtam.content.model.VowelType,
 * sm.selflearn.samskrtam.sangraha.model.Gender) — они передаются строкой (name()),
 * content-service работает со строковым представлением (String), т.к. у content-service
 * свои enum (sm.selflearn.samskrtam.content.model.VowelType/Gender), но они совпадают по
 * значениям.
 */
@Value
@Builder
public class SangrahaDeclensionExamplesRequestDto {

    String vowelType;
    String gender;
    int limitPerGroup;
    List<CellDto> cells;

    @JsonCreator
    public SangrahaDeclensionExamplesRequestDto(
            @JsonProperty("vowelType") String vowelType,
            @JsonProperty("gender") String gender,
            @JsonProperty("limitPerGroup") int limitPerGroup,
            @JsonProperty("cells") List<CellDto> cells) {
        this.vowelType = vowelType;
        this.gender = gender;
        this.limitPerGroup = limitPerGroup;
        this.cells = cells;
    }

    @Value
    @Builder
    public static class CellDto {
        String caseType;
        String numberType;

        @JsonCreator
        public CellDto(
                @JsonProperty("caseType") String caseType,
                @JsonProperty("numberType") String numberType) {
            this.caseType = caseType;
            this.numberType = numberType;
        }
    }
}