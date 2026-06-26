package sm.selflearn.samskrtam.monierwilliams.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "abbreviation", schema = "cologne_mw")
public class MwAbbreviation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entry_id", nullable = false)
    private MwEntry entry;

    @Column(name = "abbrev_text")
    private String abbrevText;

    @Column(name = "expansion")
    private String expansion;

    @Column(name = "slp1_spelling")
    private String slp1Spelling;

    @Column(name = "position_order")
    private Integer positionOrder;
}

