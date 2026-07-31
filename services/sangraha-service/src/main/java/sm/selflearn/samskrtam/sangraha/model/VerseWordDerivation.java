package sm.selflearn.samskrtam.sangraha.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.FetchType;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "verse_word_derivation", schema = "sangraha")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerseWordDerivation {

    @Id
    private UUID verseWordId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    private VerseWord verseWord;

    @Enumerated(EnumType.STRING)
    @Column(name = "derivation_type")
    private DerivationType derivationType;

    @Column(name = "derivational_suffix")
    private String derivationalSuffix;

    @Column(name = "derivational_base")
    private String derivationalBase;

    @Column(name = "description")
    private String description;
}
