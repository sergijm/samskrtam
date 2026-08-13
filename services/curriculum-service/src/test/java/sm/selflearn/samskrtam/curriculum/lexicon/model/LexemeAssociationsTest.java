package sm.selflearn.samskrtam.curriculum.lexicon.model;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class LexemeAssociationsTest {

    private SemanticTopic topic(String code) {
        SemanticTopic t = new SemanticTopic();
        t.setId(UUID.randomUUID());
        t.setCode(code);
        t.setNameRu(code);
        t.setNameEn(code);
        return t;
    }

    private PartOfSpeech pos(String code) {
        PartOfSpeech p = new PartOfSpeech();
        p.setCode(code);
        p.setGroup(PosGroup.NOMINAL);
        p.setNameRu(code);
        p.setNameEn(code);
        return p;
    }

    private MorphologyClass morphology(String code) {
        MorphologyClass m = new MorphologyClass();
        m.setCode(code);
        m.setAppliesTo(MorphologyAppliesTo.NOUN);
        m.setNameRu(code);
        m.setNameEn(code);
        return m;
    }

    @Test
    void lexeme_attachTopicsPosAndMorphology_relationsRetained() {
        Lexeme lexeme = new Lexeme();
        lexeme.setLemmaIast("nara");
        lexeme.setLemmaDevanagari("नर");
        lexeme.setLemmaSlp1("nara");
        lexeme.setGlossRu("человек");
        lexeme.setGlossEn("man");
        lexeme.setGender(LexemeGender.MASCULINE);

        lexeme.setSemanticTopics(Set.of(topic("nature"), topic("people")));
        lexeme.setPartsOfSpeech(Set.of(pos("noun")));
        lexeme.setMorphologyClasses(Set.of(morphology("a-stem-masc")));

        assertThat(lexeme.getSemanticTopics())
                .extracting(SemanticTopic::getCode)
                .containsExactlyInAnyOrder("nature", "people");
        assertThat(lexeme.getPartsOfSpeech()).extracting(PartOfSpeech::getCode).containsExactly("noun");
        assertThat(lexeme.getMorphologyClasses())
                .extracting(MorphologyClass::getCode).containsExactly("a-stem-masc");
    }
}
