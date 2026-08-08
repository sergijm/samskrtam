package sm.selflearn.samskrtam.sangraha.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sm.selflearn.samskrtam.sangraha.model.CurriculumSemanticTopic;
import sm.selflearn.samskrtam.sangraha.repository.CurriculumSemanticTopicRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LemmaClassificationValidatorTest {

    private LemmaClassificationValidator validator;

    @BeforeEach
    void setUp() {
        CurriculumSemanticTopicRepository repo = mock(CurriculumSemanticTopicRepository.class);
        CurriculumSemanticTopic animals = CurriculumSemanticTopic.builder().code("animals").build();
        when(repo.findAll()).thenReturn(List.of(animals));
        validator = new LemmaClassificationValidator(repo);
    }

    @Test
    void isValidCategoryCode_knownCode_true() {
        assertThat(validator.isValidCategoryCode("animals")).isTrue();
    }

    @Test
    void isValidCategoryCode_unknownOrBlank_false() {
        assertThat(validator.isValidCategoryCode("bogus")).isFalse();
        assertThat(validator.isValidCategoryCode("")).isFalse();
        assertThat(validator.isValidCategoryCode(null)).isFalse();
    }

    @Test
    void containsDevanagari_detectsDevanagariRange() {
        assertThat(validator.containsDevanagari("слон")).isFalse();
        assertThat(validator.containsDevanagari(null)).isFalse();
        assertThat(validator.containsDevanagari("हाथी")).isTrue();
        assertThat(validator.containsDevanagari("gaja हाथी")).isTrue();
    }
}