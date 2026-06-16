package sm.selflearn.samskrtam.eamenau.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "tasks", schema = "eamenau")
@Data
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "exercise_id", nullable = false)
    private Integer exerciseId;

    @Column(name = "task_number", nullable = false)
    private Integer taskNumber;

    @Column(name = "task_text", nullable = false)
    private String taskText;
}
