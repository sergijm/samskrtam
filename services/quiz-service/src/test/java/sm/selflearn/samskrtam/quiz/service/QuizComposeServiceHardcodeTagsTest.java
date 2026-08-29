package sm.selflearn.samskrtam.quiz.service;

import org.junit.jupiter.api.Test;
import sm.selflearn.samskrtam.quiz.model.ProgressTagSetId;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class QuizComposeServiceHardcodeTagsTest {

    @Test
    void singleCaseSet_restrictedToThatCase_allNumbersAndGenders() {
        List<String> tags = QuizComposeService.hardcodeTags(ProgressTagSetId.LOCATIVE);

        assertThat(tags).hasSize(3 * 4); // numbers x genders
        assertThat(tags).allMatch(tag -> tag.startsWith("LOCATIVE|"));
        assertThat(tags).anyMatch(tag -> tag.equals("LOCATIVE|SINGULAR|MASCULINE"));
        assertThat(tags).anyMatch(tag -> tag.equals("LOCATIVE|DUAL|FEMININE"));
        assertThat(tags).anyMatch(tag -> tag.equals("LOCATIVE|PLURAL|NEUTER"));
    }

    @Test
    void everySingleCaseSet_producesItsOwnTags() {
        for (ProgressTagSetId id : List.of(ProgressTagSetId.NOMINATIVE, ProgressTagSetId.ACCUSATIVE,
                ProgressTagSetId.INSTRUMENTAL, ProgressTagSetId.DATIVE, ProgressTagSetId.ABLATIVE,
                ProgressTagSetId.GENITIVE, ProgressTagSetId.LOCATIVE, ProgressTagSetId.VOCATIVE)) {
            List<String> tags = QuizComposeService.hardcodeTags(id);
            assertThat(tags).hasSize(12);
            assertThat(tags).allMatch(tag -> tag.startsWith(id.name() + "|"));
        }
    }

    @Test
    void numberSet_restrictedToThatNumber_allCases() {
        List<String> tags = QuizComposeService.hardcodeTags(ProgressTagSetId.DUAL);

        assertThat(tags).hasSize(8 * 4); // cases x genders
        assertThat(tags).allMatch(tag -> tag.contains("|DUAL|"));
        assertThat(tags).anyMatch(tag -> tag.equals("NOMINATIVE|DUAL|MASCULINE"));
        assertThat(tags).anyMatch(tag -> tag.equals("LOCATIVE|DUAL|UNSPECIFIED"));
    }

    @Test
    void casePairSet_restrictedToTheTwoCases() {
        List<String> tags = QuizComposeService.hardcodeTags(ProgressTagSetId.GEN_ABL);

        assertThat(tags).hasSize(2 * 3 * 4); // cases x numbers x genders
        assertThat(tags).allMatch(tag -> tag.startsWith("GENITIVE|") || tag.startsWith("ABLATIVE|"));
        assertThat(tags).anyMatch(tag -> tag.equals("GENITIVE|SINGULAR|MASCULINE"));
        assertThat(tags).anyMatch(tag -> tag.equals("ABLATIVE|PLURAL|NEUTER"));
        assertThat(tags).noneMatch(tag -> tag.startsWith("LOCATIVE|"));
        assertThat(tags).noneMatch(tag -> tag.startsWith("NOMINATIVE|"));
    }

    @Test
    void allCasePairs_coverExactlyTheirCases() {
        for (ProgressTagSetId id : List.of(ProgressTagSetId.ACC_LOC, ProgressTagSetId.INS_ABL,
                ProgressTagSetId.GEN_LOC, ProgressTagSetId.DAT_ACC, ProgressTagSetId.GEN_ABL,
                ProgressTagSetId.INS_LOC, ProgressTagSetId.DAT_GEN, ProgressTagSetId.ABL_LOC)) {
            assertThat(QuizComposeService.hardcodeTags(id)).hasSize(24);
        }
    }
}