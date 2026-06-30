package sm.selflearn.samskrtam.emenau.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "sandhi_rules_group", schema = "eamenau")
@Getter
@Setter
public class SandhiRuleGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "code")
    private String code;
}
