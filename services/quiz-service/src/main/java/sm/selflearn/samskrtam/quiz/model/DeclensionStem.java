package sm.selflearn.samskrtam.quiz.model;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Data
@Builder
@Table(name = "declension_stems", schema = "quiz")
public class DeclensionStem {

    @Id
    private UUID id;
    private String stemIast;
    private String stemDevanagari;
    private String translationRu;
    private String translationEn;
    private String gender;
    private String vowelType;
}