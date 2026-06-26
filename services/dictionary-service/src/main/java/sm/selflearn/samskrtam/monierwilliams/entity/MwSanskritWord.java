package sm.selflearn.samskrtam.monierwilliams.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "sanskrit_word", schema = "cologne_mw")
public class MwSanskritWord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "entry_id")
    private Integer entryId;

    @Column(name = "slp1_spelling")
    private String slp1Spelling;

    @Column(name = "slp1_normalized")
    private String slp1Normalized;

    @Column(name = "iast_spelling")
    private String iastSpelling;

    @Column(name = "is_primary_headword")
    private Boolean isPrimaryHeadword;

    @Column(name = "position_order")
    private Integer positionOrder;
}

