package sm.selflearn.samskrtam.eamenau.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Entity
@Table(name = "place_of_articulation", schema = "eamenau")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlaceOfArticulation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "latin_name", nullable = false, length = 50)
    private String latinName;

    @Column(name = "latin_abbr", nullable = false, length = 10)
    private String latinAbbr;

    @Column(name = "english_name", nullable = false, length = 50)
    private String englishName;

    @Column(name = "russian_name", nullable = false, length = 50)
    private String russianName;
}
