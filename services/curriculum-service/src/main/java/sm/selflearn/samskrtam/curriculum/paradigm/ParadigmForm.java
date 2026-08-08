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
import sm.selflearn.samskrtam.content.model.NumberType;

import java.util.UUID;

/**
 * One suppletive paradigm cell (case+number -> form), attached to
 * {@link ParadigmStem}. Mirrors {@code content.declension_forms}.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "declension_form", schema = "curriculum")
@IdClass(ParadigmFormId.class)
public class ParadigmForm {

    @Id
    @Column(name = "declension_stem_id", nullable = false)
    private UUID declensionStemId;

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