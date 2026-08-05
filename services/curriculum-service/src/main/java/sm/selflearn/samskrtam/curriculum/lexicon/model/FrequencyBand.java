package sm.selflearn.samskrtam.curriculum.lexicon.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "frequency_band", schema = "curriculum")
public class FrequencyBand {
    @Id
    @Column(name = "code", length = 20)
    private String code;

    @Column(name = "min_rank", nullable = false)
    private Integer minRank;

    @Column(name = "max_rank", nullable = false)
    private Integer maxRank;

    @Column(name = "label_ru", nullable = false, length = 60)
    private String labelRu;

    @Column(name = "label_en", nullable = false, length = 60)
    private String labelEn;

    @Column(name = "sort_order", nullable = false)
    private Short sortOrder;
}