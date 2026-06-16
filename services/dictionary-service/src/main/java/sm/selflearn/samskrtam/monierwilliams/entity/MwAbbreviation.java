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

    @Column(name = "entry_id")
    private Integer entryId;

    @Column(name = "abbrev_text")
    private String abbrevText;

    @Column(name = "expansion")
    private String expansion;

    @Column(name = "slp1_spelling")
    private String slp1Spelling;

    @Column(name = "position_order")
    private Integer positionOrder;
}
