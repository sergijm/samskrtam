package sm.selflearn.samskrtam.content.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

/**
 * Page DTO for the index-based conjugation paradigm carousel.
 * <p>
 * Response of {@code GET /api/v2/curriculum/topics/{topicCode}/conjugation-paradigms?index=N}.
 * Returns exactly ONE verb lemma (with all its example sentences) plus carousel
 * metadata (index, totalCount) so the frontend can render "N / total" and
 * disable arrows.
 */
@Value
@Builder
public class ConjugationParadigmPageDto {
    /** 0-based position of the returned verb in the sorted lemma list. */
    int index;
    /** Total number of verb lemmas for this lesson. */
    int totalCount;
    /** The current verb paradigm. */
    ConjugationParadigmDto paradigm;

    @JsonCreator
    public ConjugationParadigmPageDto(
            @JsonProperty("index") int index,
            @JsonProperty("totalCount") int totalCount,
            @JsonProperty("paradigm") ConjugationParadigmDto paradigm) {
        this.index = index;
        this.totalCount = totalCount;
        this.paradigm = paradigm;
    }
}