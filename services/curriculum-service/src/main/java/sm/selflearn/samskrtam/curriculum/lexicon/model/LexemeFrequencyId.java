package sm.selflearn.samskrtam.curriculum.lexicon.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Data;

import java.io.Serializable;
import java.util.UUID;

@Data
@Embeddable
public class LexemeFrequencyId implements Serializable {
    @Column(name = "lexeme_id", nullable = false)
    private UUID lexemeId;

    @Column(name = "source", nullable = false, length = 50)
    private String source;
}