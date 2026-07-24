package sm.selflearn.samskrtam.content.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

/**
 * Page DTO for the index-based declension paradigm carousel.
 * <p>
 * Response of {@code GET /content/public/lessons/{slug}/declension-paradigms?index=N}.
 * Returns exactly ONE paradigm (stem + all its forms) plus carousel metadata
 * (index, totalCount) so the frontend can render "N / total" and disable arrows.
 */
@Value
@Builder
public class DeclensionParadigmPageDto {
    /** 0-based position of the returned paradigm in the sorted stem list. */
    int index;
    /** Total number of stems for this lesson. */
    int totalCount;
    /** The current paradigm. */
    DeclensionParadigmDto paradigm;

    @JsonCreator
    public DeclensionParadigmPageDto(
            @JsonProperty("index") int index,
            @JsonProperty("totalCount") int totalCount,
            @JsonProperty("paradigm") DeclensionParadigmDto paradigm) {
        this.index = index;
        this.totalCount = totalCount;
        this.paradigm = paradigm;
    }
}
