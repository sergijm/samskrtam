package sm.selflearn.samskrtam.content.eamenau.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Entity
@Table(name = "sandhi_rules", schema = "eamenau")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SandhiRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "rule_number", nullable = false)
    private Integer ruleNumber;

    @Column(name = "rule_type", nullable = false, length = 20)
    private String ruleType;

    @Column(name = "short_description")
    private String shortDescription;

    @Column(name = "whitney_number", length = 20)
    private String whitneyNumber;

    @Column(name = "iast_example")
    private String iastExample;

    @Column(name = "hk_example")
    private String hkExample;

    @Column(name = "notes")
    private String notes;

    @Column(name = "full_text", nullable = false)
    private String fullText;
}
