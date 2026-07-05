package sm.selflearn.samskrtam.sangraha.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "chapters", schema = "sangraha")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Chapter {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "work_id", nullable = false)
    private UUID workId;

    @Column(nullable = false)
    private String slug;

    @Column(name = "order_index")
    private Integer orderIndex;

    @Column(name = "title_ru", nullable = false)
    private String titleRu;

    @Column(name = "title_en", nullable = false)
    private String titleEn;

    @Column(name = "title_sa_iast")
    private String titleSaIast;

    @Column(name = "title_sa_devanagari")
    private String titleSaDevanagari;

    @Column(name = "deleted_at")
    private Instant deletedAt;
}