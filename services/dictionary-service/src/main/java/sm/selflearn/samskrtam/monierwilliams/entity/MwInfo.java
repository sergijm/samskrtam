package sm.selflearn.samskrtam.monierwilliams.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "info", schema = "cologne_mw")
public class MwInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "entry_id")
    private Integer entryId;

    @Column(name = "info_type")
    private String infoType;

    @Column(name = "info_value")
    private String infoValue;

    @Column(name = "verb_cp")
    private String verbCp;

    @Column(name = "verb_parse")
    private String verbParse;

    @Column(name = "westergaard_root")
    private String westergaardRoot;

    @Column(name = "westergaard_section")
    private String westergaardSection;

    @Column(name = "westergaard_sayana_ref")
    private String westergaardSayanaRef;

    @Column(name = "whitney_root")
    private String whitneyRoot;

    @Column(name = "whitney_page")
    private String whitneyPage;
}
