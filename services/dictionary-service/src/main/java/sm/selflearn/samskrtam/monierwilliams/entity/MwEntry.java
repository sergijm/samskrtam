package sm.selflearn.samskrtam.monierwilliams.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "entry", schema = "cologne_mw")
public class MwEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "record_id_full")
    private String recordIdFull;

    @Column(name = "key1")
    private String key1;

    @Column(name = "key1_normalized")
    private String key1Normalized;

    @Column(name = "key1_iast")
    private String key1Iast;

    @Column(name = "key1_iast_plain")
    private String key1IastPlain;

    @Column(name = "key2")
    private String key2;

    @Column(name = "homonym_num")
    private String homonymNum;

    @Column(name = "e_code")
    private String eCode;

    @Column(name = "page")
    private Integer page;

    @Column(name = "column_num")
    private Integer columnNum;

    @Column(name = "is_supplement")
    private Boolean isSupplement;

    @Column(name = "body", columnDefinition = "TEXT")
    private String body;
}
