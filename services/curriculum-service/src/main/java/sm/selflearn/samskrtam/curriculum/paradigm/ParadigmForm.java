package sm.selflearn.samskrtam.curriculum.paradigm;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import sm.selflearn.samskrtam.content.model.CaseType;
import sm.selflearn.samskrtam.morphology.NumberType;
import sm.selflearn.samskrtam.content.model.VowelType;

/**
 * One paradigm cell (case+number -> form) of a lemma's declension class, keyed by
 * {@code (lemma_iast, vowel_type, case_type, number_type)}. Serves both the v2
 * paradigm page and the batch generator.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "declension_form", schema = "curriculum")
@IdClass(ParadigmFormId.class)
public class ParadigmForm {

    @Id
    @Column(name = "lemma_iast", nullable = false, length = 120)
    private String lemmaIast;

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "vowel_type", nullable = false, length = 40)
    private VowelType vowelType;

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "case_type", nullable = false, length = 40)
    private CaseType caseType;

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "number_type", nullable = false, length = 40)
    private NumberType numberType;

    @Column(name = "form_iast", length = 120)
    private String formIast;

    @Column(name = "form_devanagari", length = 120)
    private String formDevanagari;
}
