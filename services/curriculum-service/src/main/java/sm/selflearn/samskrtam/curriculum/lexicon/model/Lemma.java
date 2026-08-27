package sm.selflearn.samskrtam.curriculum.lexicon.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "lemma", schema = "curriculum")
public class Lemma {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "lemma_iast", nullable = false, unique = true, length = 120)
    private String lemmaIast;

    @Column(name = "pos", length = 40)
    private String pos;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 20)
    private LexemeGender gender;

    @Column(name = "freq_order")
    private Integer freqOrder;
}