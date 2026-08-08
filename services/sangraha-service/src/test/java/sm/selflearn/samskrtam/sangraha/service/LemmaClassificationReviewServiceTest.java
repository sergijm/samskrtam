package sm.selflearn.samskrtam.sangraha.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sm.selflearn.samskrtam.sangraha.dto.LemmaClassificationItemDto;
import sm.selflearn.samskrtam.sangraha.dto.LemmaClassificationPageDto;
import sm.selflearn.samskrtam.sangraha.dto.LemmaClassificationReviewRequest;
import sm.selflearn.samskrtam.sangraha.model.ClassificationStatus;
import sm.selflearn.samskrtam.sangraha.model.Lemma;
import sm.selflearn.samskrtam.sangraha.model.LemmaClassification;
import sm.selflearn.samskrtam.sangraha.repository.LemmaClassificationRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LemmaClassificationReviewServiceTest {

    private LemmaClassificationRepository repo;
    private LemmaClassificationValidator validator;
    private LemmaClassificationReviewService service;

    private final UUID id = UUID.randomUUID();
    private Lemma lemma;
    private LemmaClassification row;

    @BeforeEach
    void setUp() {
        repo = mock(LemmaClassificationRepository.class);
        validator = mock(LemmaClassificationValidator.class);
        service = new LemmaClassificationReviewService(repo, validator);

        lemma = Lemma.builder().id(UUID.randomUUID()).lemmaSlp1("gaja").lemmaIast("gaja")
                .lemmaDevanagari("ग्ज").occurrenceCount(5).frequencyRank(1)
                .dominantPosCode("NOUN").build();
        row = LemmaClassification.builder().id(id).lemma(lemma).schemeCode("CURRICULUM")
                .status(ClassificationStatus.CANDIDATE).build();

        when(repo.findById(id)).thenReturn(Optional.of(row));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(validator.isValidCategoryCode("animals")).thenReturn(true);
    }

    @Test
    void review_approveWithoutCategoryCode_rejected() {
        assertThatThrownBy(() -> service().review(id, "CURRICULUM",
                new LemmaClassificationReviewRequest("APPROVED", null, null, null, null), "admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("APPROVED requires a valid categoryCode");
    }

    @Test
    void review_approveWithValidCategoryCode_setsReviewedByAndAt() {
        row.setCategoryCode("animals");
        LemmaClassificationItemDto dto = service().review(id, "CURRICULUM",
                new LemmaClassificationReviewRequest("APPROVED", null, null, null, null), "admin");

        assertThat(dto.status()).isEqualTo("APPROVED");
        assertThat(dto.reviewedBy()).isEqualTo("admin");
        assertThat(dto.reviewedAt()).isNotNull();
    }

    @Test
    void review_plainFieldUpdate_doesNotSetReviewer() {
        LemmaClassificationItemDto dto = service().review(id, "CURRICULUM",
                new LemmaClassificationReviewRequest(null, null, "слон", null, null), "admin");

        assertThat(dto.glossRu()).isEqualTo("слон");
        assertThat(dto.reviewedBy()).isNull();
        assertThat(dto.reviewedAt()).isNull();
        assertThat(dto.status()).isEqualTo("CANDIDATE");
    }

    @Test
    void listForReview_returnsRowsAndNextCursor() {
        when(repo.findForReview(any(), any(), any(), any())).thenReturn(List.of(row));
        LemmaClassificationPageDto page = service().listForReview("CURRICULUM",
                ClassificationStatus.CANDIDATE, null, 50);

        assertThat(page.items()).hasSize(1);
        assertThat(page.nextCursor()).isEqualTo(lemma.getId());
    }

    @Test
    void review_unknownCategoryCode_onStatusChange_rejected() {
        when(validator.isValidCategoryCode("bogus")).thenReturn(false);
        assertThatThrownBy(() -> service().review(id, "CURRICULUM",
                new LemmaClassificationReviewRequest("APPROVED", "bogus", null, null, null), "admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown categoryCode");
    }

    private LemmaClassificationReviewService service() {
        return new LemmaClassificationReviewService(repo, validator);
    }
}