package sm.selflearn.samskrtam.curriculum.paradigm;

import org.junit.jupiter.api.Test;
import sm.selflearn.samskrtam.content.model.VowelType;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DeclensionClassMapperTest {

    @Test
    void topicToClassCodes_topicMergingSeveralClasses_returnsAll() {
        assertThat(DeclensionClassMapper.topicToClassCodes("a-stem"))
                .containsExactlyInAnyOrder("a-stem-masc", "a-stem-neut");
        assertThat(DeclensionClassMapper.topicToClassCodes("i-u-stems"))
                .containsExactlyInAnyOrder("i-stem", "u-stem");
        assertThat(DeclensionClassMapper.topicToClassCodes("ii-uu-stems"))
                .containsExactlyInAnyOrder("ii-stem", "uu-stem");
    }

    @Test
    void topicToClassCodes_topicIsItselfAClassCode_returnsItself() {
        assertThat(DeclensionClassMapper.topicToClassCodes("a-stem-masc")).containsExactly("a-stem-masc");
        assertThat(DeclensionClassMapper.topicToClassCodes("i-stem")).containsExactly("i-stem");
    }

    @Test
    void topicToClassCodes_pronounTopics_haveNoMorphologyClassCodes() {
        assertThat(DeclensionClassMapper.topicToClassCodes("personal-pronouns")).isEmpty();
        assertThat(DeclensionClassMapper.topicToClassCodes("demonstrative-pronouns")).isEmpty();
        assertThat(DeclensionClassMapper.topicToClassCodes("no-such-topic")).isEmpty();
        assertThat(DeclensionClassMapper.topicToClassCodes(null)).isEmpty();
    }

    @Test
    void topicToVowelTypes_mapsNounAndPronounTopics() {
        assertThat(DeclensionClassMapper.topicToVowelTypes("a-stem"))
                .containsExactly(VowelType.A_STEM);
        assertThat(DeclensionClassMapper.topicToVowelTypes("i-u-stems"))
                .containsExactlyInAnyOrder(VowelType.I_STEM, VowelType.U_STEM);
        assertThat(DeclensionClassMapper.topicToVowelTypes("personal-pronouns"))
                .containsExactlyInAnyOrder(VowelType.PRON_ASMAD, VowelType.PRON_YUSMAD);
        assertThat(DeclensionClassMapper.topicToVowelTypes("demonstrative-pronouns"))
                .containsExactlyInAnyOrder(
                        VowelType.PRON_TAD_MASC, VowelType.PRON_TAD_NEUT, VowelType.PRON_TAD_FEM,
                        VowelType.PRON_IDAM_MASC, VowelType.PRON_IDAM_NEUT, VowelType.PRON_IDAM_FEM,
                        VowelType.PRON_ADAS_MASC, VowelType.PRON_ADAS_NEUT, VowelType.PRON_ADAS_FEM);
        assertThat(DeclensionClassMapper.topicToVowelTypes("interrogative-pronouns"))
                .containsExactlyInAnyOrder(VowelType.PRON_TAD_MASC, VowelType.PRON_TAD_NEUT, VowelType.PRON_TAD_FEM);
        assertThat(DeclensionClassMapper.topicToVowelTypes("relative-pronouns"))
                .containsExactlyInAnyOrder(VowelType.PRON_TAD_MASC, VowelType.PRON_TAD_NEUT, VowelType.PRON_TAD_FEM);
        assertThat(DeclensionClassMapper.topicToVowelTypes("indefinite-pronouns"))
                .containsExactlyInAnyOrder(VowelType.PRON_TAD_MASC, VowelType.PRON_TAD_NEUT, VowelType.PRON_TAD_FEM);
        assertThat(DeclensionClassMapper.topicToVowelTypes("reflexive-possessive-pronouns"))
                .containsExactlyInAnyOrder(VowelType.PRON_AN, VowelType.PRON_VAT_MASC, VowelType.PRON_VAT_FEM,
                        VowelType.A_STEM, VowelType.AA_STEM);
        assertThat(DeclensionClassMapper.topicToVowelTypes("pronominal-adjectives"))
                .containsExactlyInAnyOrder(
                        VowelType.PRON_SARVA_MASC, VowelType.PRON_SARVA_NEUT, VowelType.PRON_SARVA_FEM,
                        VowelType.PRON_PURVA_MASC, VowelType.PRON_PURVA_NEUT, VowelType.PRON_PURVA_FEM);
        assertThat(DeclensionClassMapper.topicToVowelTypes("quantifier-pronouns"))
                .containsExactlyInAnyOrder(VowelType.PRON_UBHA_MASC, VowelType.PRON_UBHA_FN);
    }

    @Test
    void isRegularDeclensionTopic_regularAndPronounTopics_returnTrue() {
        assertThat(DeclensionClassMapper.isRegularDeclensionTopic("a-stem")).isTrue();
        assertThat(DeclensionClassMapper.isRegularDeclensionTopic("i-u-stems")).isTrue();
        assertThat(DeclensionClassMapper.isRegularDeclensionTopic("a-stem-masc")).isTrue();
        assertThat(DeclensionClassMapper.isRegularDeclensionTopic("demonstrative-pronouns")).isTrue();
        assertThat(DeclensionClassMapper.isRegularDeclensionTopic("pronominal-adjectives")).isTrue();
        assertThat(DeclensionClassMapper.isRegularDeclensionTopic("personal-pronouns")).isTrue();
        assertThat(DeclensionClassMapper.isRegularDeclensionTopic("no-such-topic")).isFalse();
    }

    @Test
    void toVowelType_mergedAStemAndConcreteClasses_allMapToA_STEM() {
        assertThat(DeclensionClassMapper.toVowelType("a-stem")).isEqualTo(VowelType.A_STEM);
        assertThat(DeclensionClassMapper.toVowelType("a-stem-masc")).isEqualTo(VowelType.A_STEM);
        assertThat(DeclensionClassMapper.toVowelType("a-stem-neut")).isEqualTo(VowelType.A_STEM);
        assertThat(DeclensionClassMapper.toVowelType("a-stem-fem")).isEqualTo(VowelType.AA_STEM);
        assertThat(DeclensionClassMapper.toVowelType("ii-stem")).isEqualTo(VowelType.II_STEM);
        assertThat(DeclensionClassMapper.toVowelType("uu-stem")).isEqualTo(VowelType.UU_STEM);
    }

    @Test
    void toVowelType_unknownCode_returnsNull() {
        assertThat(DeclensionClassMapper.toVowelType("consonant-stem")).isNull();
        assertThat(DeclensionClassMapper.toVowelType("no-such-class")).isNull();
    }

    @Test
    void topic_coveringSeveralClasses_yieldsSeveralVowelTypes() {
        List<VowelType> vowelTypes = DeclensionClassMapper.topicToClassCodes("i-u-stems").stream()
                .map(DeclensionClassMapper::toVowelType)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        assertThat(vowelTypes).containsExactlyInAnyOrder(VowelType.I_STEM, VowelType.U_STEM);
    }
}
