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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entry_id", nullable = false)
    private MwEntry entry;

    @Column(name = "source_ref")
    private String sourceRef;

    @Column(name = "position_order")
    private Integer positionOrder;
}

