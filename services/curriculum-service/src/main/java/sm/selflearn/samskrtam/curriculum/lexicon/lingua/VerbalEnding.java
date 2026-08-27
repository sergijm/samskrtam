package sm.selflearn.samskrtam.curriculum.lexicon.lingua;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "verbal_endings", schema = "lingua")
public class VerbalEnding {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "verbal_endings_id_seq")
    @SequenceGenerator(name = "verbal_endings_id_seq", sequenceName = "verbal_endings_id_seq", schema = "lingua", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @Column(name = "ending", nullable = false, length = 32)
    private String ending;

    @Column(name = "lemma_suffix", nullable = false, length = 8)
    private String lemmaSuffix;

    @Column(name = "has_augment", nullable = false)
    private Boolean hasAugment;

    @Column(name = "tense_mood", nullable = false, length = 20)
    private String tenseMood;

    @Column(name = "person_number", nullable = false, length = 16)
    private String personNumber;

    @Column(name = "pada", nullable = false, length = 8)
    private String pada;

    @Column(name = "notes", length = 255)
    private String notes;
}