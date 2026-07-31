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
@Table(name = "verse_word_morphology", schema = "sangraha")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerseWordMorphology {

    @Id
    private UUID verseWordId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    private VerseWord verseWord;

    @Enumerated(EnumType.STRING)
    @Column(name = "case_type")
    private GrammaticalCase caseType;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender")
    private Gender gender;

    @Enumerated(EnumType.STRING)
    @Column(name = "number_type")
    private NumberType numberType;

    @Enumerated(EnumType.STRING)
    @Column(name = "person")
    private Person person;

    @Enumerated(EnumType.STRING)
    @Column(name = "tense")
    private Tense tense;

    @Enumerated(EnumType.STRING)
    @Column(name = "mood")
    private Mood mood;

    @Enumerated(EnumType.STRING)
    @Column(name = "voice")
    private Voice voice;
}
