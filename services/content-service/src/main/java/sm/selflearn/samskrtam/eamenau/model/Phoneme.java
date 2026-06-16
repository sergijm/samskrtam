package sm.selflearn.samskrtam.eamenau.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Entity
@Table(name = "phonemes", schema = "eamenau")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Phoneme {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "iast_symbol", nullable = false, length = 10)
    private String iastSymbol;

    @Column(name = "harvard_kyoto_symbol", nullable = false, length = 10)
    private String harvardKyotoSymbol;

    @Column(name = "devanagari_symbol", nullable = false, length = 10)
    private String devanagariSymbol;

    @Column(length = 50)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id")
    private PlaceOfArticulation place;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manner_id")
    private MannerOfArticulation manner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voicing_id")
    private Voicing voicing;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aspiration_id")
    private Aspiration aspiration;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "varga_id")
    private Varga varga;

    @Column(name = "is_nasal")
    private Boolean isNasal = false;
}
