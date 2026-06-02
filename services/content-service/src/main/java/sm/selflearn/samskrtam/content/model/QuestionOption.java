package sm.selflearn.samskrtam.content.model;

import jakarta.persistence.*;
import lombok.Data;
import java.util.UUID;

@Data
@Entity
@Table(name = "question_options", schema = "content")
public class QuestionOption {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "question_id", nullable = false)
    private UUID questionId;

    @Column(name = "form_iast", nullable = false) // Changed from textRu
    private String formIast;

    @Column(name = "form_devanagari") // Changed from textEn
    private String formDevanagari;
}
