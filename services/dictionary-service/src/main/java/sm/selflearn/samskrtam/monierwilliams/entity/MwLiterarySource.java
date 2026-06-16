package sm.selflearn.samskrtam.monierwilliams.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "literary_source", schema = "cologne_mw")
public class MwLiterarySource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "entry_id")
    private Integer entryId;

    @Column(name = "source_ref")
    private String sourceRef;

    @Column(name = "position_order")
    private Integer positionOrder;
}
