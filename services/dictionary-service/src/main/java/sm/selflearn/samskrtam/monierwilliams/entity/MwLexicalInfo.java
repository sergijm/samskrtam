package sm.selflearn.samskrtam.monierwilliams.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "lexical_info", schema = "cologne_mw")
public class MwLexicalInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entry_id", nullable = false)
    private MwEntry entry;

    @Column(name = "lex_type")
    private String lexType;

    @Column(name = "gender_standard")
    private String genderStandard;

    @Column(name = "gender_raw")
    private String genderRaw;
}

