package sm.selflearn.samskrtam.eamenau.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "solutions", schema = "eamenau")
@Data
public class Solution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "task_id", nullable = false)
    private Integer taskId;

    @Column(name = "solution_text", nullable = false)
    private String solutionText;

    @Column(name = "step_by_step")
    private String stepByStep;

    @Column(name = "is_correct", nullable = false)
    private boolean isCorrect;
}
