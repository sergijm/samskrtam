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

    @Column(name = "entry_id")
    private Integer entryId;

    @Column(name = "lex_type")
    private String lexType;

    @Column(name = "gender_standard")
    private String genderStandard;

    @Column(name = "gender_raw")
    private String genderRaw;
}
