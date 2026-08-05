package sm.selflearn.samskrtam.curriculum.lexicon.model;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "lexeme_frequency", schema = "curriculum")
public class LexemeFrequency {
    @EmbeddedId
    private LexemeFrequencyId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("lexemeId")
    @JoinColumn(name = "lexeme_id", nullable = false)
    private Lexeme lexeme;

    @Column(name = "rank", nullable = false)
    private Integer rank;
}