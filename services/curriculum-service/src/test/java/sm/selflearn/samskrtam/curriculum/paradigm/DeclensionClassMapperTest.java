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
    void topicToClassCodes_unknownOrNull_returnsEmpty() {
        assertThat(DeclensionClassMapper.topicToClassCodes("personal-pronouns")).isEmpty();
        assertThat(DeclensionClassMapper.topicToClassCodes("no-such-topic")).isEmpty();
        assertThat(DeclensionClassMapper.topicToClassCodes(null)).isEmpty();
    }

    @Test
    void isRegularDeclensionTopic_regularClasses_returnsTrue() {
        assertThat(DeclensionClassMapper.isRegularDeclensionTopic("a-stem")).isTrue();
        assertThat(DeclensionClassMapper.isRegularDeclensionTopic("i-u-stems")).isTrue();
        assertThat(DeclensionClassMapper.isRegularDeclensionTopic("a-stem-masc")).isTrue();
        assertThat(DeclensionClassMapper.isRegularDeclensionTopic("personal-pronouns")).isFalse();
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