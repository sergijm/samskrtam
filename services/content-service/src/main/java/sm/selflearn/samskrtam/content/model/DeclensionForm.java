package sm.selflearn.samskrtam.content.model;

import jakarta.persistence.*;
import lombok.Data;
import java.util.UUID;

@Data
@Entity
@Table(name = "declension_forms", schema = "content")
@IdClass(DeclensionFormId.class) // Composite primary key
public class DeclensionForm {
    @Id
    @Column(name = "declension_stem_id", nullable = false)
    private UUID declensionStemId;

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "case_type", nullable = false)
    private CaseType caseType;

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "number_type", nullable = false)
    private NumberType numberType;

    @Column(name = "form_iast", nullable = false)
    private String formIast;

    @Column(name = "form_devanagari")
    private String formDevanagari;
}
