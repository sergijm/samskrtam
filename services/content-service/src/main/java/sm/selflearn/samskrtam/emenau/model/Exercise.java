package sm.selflearn.samskrtam.emenau.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "exercises", schema = "eamenau")
@Data
public class Exercise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "exercise_number", nullable = false)
    private Integer exerciseNumber;

    @Column(name = "exercise_letter")
    private String exerciseLetter;

    @Column(name = "instruction_text", nullable = false)
    private String instructionText;
}
