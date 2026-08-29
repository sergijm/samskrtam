package sm.selflearn.samskrtam.curriculum.lexicon.lingua;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import sm.selflearn.samskrtam.curriculum.lexicon.model.LexemeGender;
import sm.selflearn.samskrtam.curriculum.questgen.morphology.CaseType;
import sm.selflearn.samskrtam.morphology.NumberType;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "case_endings", schema = "lingua")
public class CaseEnding {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "case_endings_id_seq")
    @SequenceGenerator(name = "case_endings_id_seq", sequenceName = "case_endings_id_seq", schema = "lingua", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(name = "stem_type", nullable = false, length = 30)
    private StemTypeEnum stemType;

    @Enumerated(EnumType.STRING)
    @Column(name = "pos", nullable = false, length = 20)
    private PosEnum pos;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false, length = 15)
    private LexemeGender gender;

    @Enumerated(EnumType.STRING)
    @Column(name = "number", nullable = false, length = 10)
    private NumberType number;

    @Enumerated(EnumType.STRING)
    @Column(name = "grammatical_case", nullable = false, length = 15)
    private CaseType grammaticalCase;

    @Column(name = "case_ending", nullable = false, length = 64)
    private String caseEnding;
}
