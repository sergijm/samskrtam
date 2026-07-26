package sm.selflearn.samskrtam.content.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import sm.selflearn.samskrtam.content.model.VowelType;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SlugToVowelTypeMapperTest {

    // ─── Compound slugs ──────────────────────────────────────────────

    @Test
    void mapSlugToVowelTypes_declensions_i_u_shouldReturnIAndUStem() {
        List<VowelType> result = SlugToVowelTypeMapper.mapSlugToVowelTypes("declensions-i-u");
        assertThat(result).containsExactly(VowelType.I_STEM, VowelType.U_STEM);
    }

    @Test
    void mapSlugToVowelTypes_declensions_ii_uu_shouldReturnIIAndUUStem() {
        List<VowelType> result = SlugToVowelTypeMapper.mapSlugToVowelTypes("declensions-ii-uu");
        assertThat(result).containsExactly(VowelType.II_STEM, VowelType.UU_STEM);
    }

    // ─── Legacy single-type slugs ────────────────────────────────────

    @ParameterizedTest
    @CsvSource({
            "declensions-a-masc, A_STEM",
            "declensions-a-neut, A_STEM",
            "declensions-a-fem,  AA_STEM",
            "declensions-aa-fem, AA_STEM",
            "declensions-i,      I_STEM",
            "declensions-ii,     II_STEM",
            "declensions-ii-fem, II_STEM",
            "declensions-u,      U_STEM",
            "declensions-uu,     UU_STEM",
            "declensions-uu-fem, UU_STEM",
            "declensions-r,      R_STEM",
    })
    void mapSlugToVowelTypes_legacySlug_shouldReturnSingleType(String slug, VowelType expected) {
        List<VowelType> result = SlugToVowelTypeMapper.mapSlugToVowelTypes(slug);
        assertThat(result).containsExactly(expected);
    }

    @Test
    void mapSlugToVowelTypes_nullSlug_shouldReturnEmptyList() {
        assertThat(SlugToVowelTypeMapper.mapSlugToVowelTypes(null)).isEmpty();
    }

    @Test
    void mapSlugToVowelTypes_declensions_all_shouldReturnEmptyList() {
        assertThat(SlugToVowelTypeMapper.mapSlugToVowelTypes("declensions-all")).isEmpty();
    }

    // ─── isUnspecifiedGenderType ─────────────────────────────────────

    @Test
    void isUnspecifiedGenderType_iStem_shouldReturnTrue() {
        assertThat(SlugToVowelTypeMapper.isUnspecifiedGenderType(List.of(VowelType.I_STEM))).isTrue();
    }

    @Test
    void isUnspecifiedGenderType_iAndUStem_shouldReturnTrue() {
        assertThat(SlugToVowelTypeMapper.isUnspecifiedGenderType(
                List.of(VowelType.I_STEM, VowelType.U_STEM))).isTrue();
    }

    @Test
    void isUnspecifiedGenderType_iiAndUUStem_shouldReturnTrue() {
        assertThat(SlugToVowelTypeMapper.isUnspecifiedGenderType(
                List.of(VowelType.II_STEM, VowelType.UU_STEM))).isTrue();
    }

    @Test
    void isUnspecifiedGenderType_aStem_shouldReturnFalse() {
        assertThat(SlugToVowelTypeMapper.isUnspecifiedGenderType(List.of(VowelType.A_STEM))).isFalse();
    }

    @Test
    void isUnspecifiedGenderType_aaStem_shouldReturnFalse() {
        assertThat(SlugToVowelTypeMapper.isUnspecifiedGenderType(List.of(VowelType.AA_STEM))).isFalse();
    }

    @Test
    void isUnspecifiedGenderType_emptyList_shouldReturnFalse() {
        assertThat(SlugToVowelTypeMapper.isUnspecifiedGenderType(List.of())).isFalse();
    }

    @Test
    void isUnspecifiedGenderType_null_shouldReturnFalse() {
        assertThat(SlugToVowelTypeMapper.isUnspecifiedGenderType(null)).isFalse();
    }

    // ─── Deprecated mapSlugToVowelType ───────────────────────────────

    @Test
    void mapSlugToVowelType_compoundIExtends_shouldReturnFirstType() {
        assertThat(SlugToVowelTypeMapper.mapSlugToVowelType("declensions-i-u"))
                .isEqualTo(VowelType.I_STEM);
    }

    @Test
    void mapSlugToVowelType_compoundIIExtends_shouldReturnFirstType() {
        assertThat(SlugToVowelTypeMapper.mapSlugToVowelType("declensions-ii-uu"))
                .isEqualTo(VowelType.II_STEM);
    }

    @Test
    void mapSlugToVowelType_unknown_shouldReturnNull() {
        assertThat(SlugToVowelTypeMapper.mapSlugToVowelType("declensions-all")).isNull();
    }
}
