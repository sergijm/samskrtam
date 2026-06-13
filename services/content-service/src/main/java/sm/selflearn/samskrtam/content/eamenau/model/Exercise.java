package sm.selflearn.samskrtam.content.eamenau.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Entity
@Table(name = "exercises", schema = "eamenau")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Exercise {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "exercise_number", nullable = false)
    private Integer exerciseNumber;

    @Column(name = "exercise_letter", length = 10)
    private String exerciseLetter;

    @Column(name = "instruction_text", nullable = false)
    private String instructionText;
}
