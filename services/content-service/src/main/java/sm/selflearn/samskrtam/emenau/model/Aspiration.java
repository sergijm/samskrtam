package sm.selflearn.samskrtam.emenau.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Entity
@Table(name = "aspiration", schema = "eamenau")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Aspiration {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "latin_name", nullable = false, length = 30)
    private String latinName;

    @Column(name = "latin_abbr", nullable = false, length = 10)
    private String latinAbbr;

    @Column(name = "english_name", nullable = false, length = 30)
    private String englishName;

    @Column(name = "russian_name", nullable = false, length = 30)
    private String russianName;
}
