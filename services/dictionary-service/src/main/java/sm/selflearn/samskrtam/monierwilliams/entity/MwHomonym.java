package sm.selflearn.samskrtam.monierwilliams.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "homonym", schema = "cologne_mw")
public class MwHomonym {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "entry_id")
    private Integer entryId;

    @Column(name = "homonym_number")
    private String homonymNumber;

    @Column(name = "homonym_text")
    private String homonymText;

    @Column(name = "position_order")
    private Integer positionOrder;
}
