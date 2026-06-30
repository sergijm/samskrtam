package sm.selflearn.samskrtam.content.model;

import jakarta.persistence.*;
import lombok.Data;
import java.util.UUID;

@Entity
@Table(name = "case_endings", schema = "content")
@Data
public class CaseEnding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "vowel_type", nullable = false)
    private VowelType vowelType;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false)
    private Gender gender;

    @Enumerated(EnumType.STRING)
    @Column(name = "case_type", nullable = false)
    private CaseType caseType;

    @Enumerated(EnumType.STRING)
    @Column(name = "number_type", nullable = false)
    private NumberType numberType;

    @Column(name = "ending_iast", nullable = false)
    private String endingIast;

    @Column(name = "ending_devanagari")
    private String endingDevanagari;
}