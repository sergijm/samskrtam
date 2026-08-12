package sm.selflearn.samskrtam.curriculum.lexicon.service;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import sm.selflearn.samskrtam.curriculum.lexicon.dto.LexemeDetailDto;
import sm.selflearn.samskrtam.curriculum.lexicon.model.Lexeme;
import sm.selflearn.samskrtam.curriculum.lexicon.model.LexemeGender;
import sm.selflearn.samskrtam.curriculum.lexicon.model.LexemeStatus;
import sm.selflearn.samskrtam.curriculum.lexicon.model.PartOfSpeech;
import sm.selflearn.samskrtam.curriculum.lexicon.model.PosGroup;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.LexemeFrequencyRepository;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.LexemeRepository;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.MorphologyClassRepository;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.PartOfSpeechRepository;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.SemanticTopicRepository;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LexemeAdminServiceTest {

    private Lexeme nominee() {
        Lexeme l = new Lexeme();
        l.setId(UUID.randomUUID());
        l.setLemmaIast("nara");
        l.setLemmaDevanagari("नर");
        l.setLemmaSlp1("nara");
        l.setGlossRu("человек");
        l.setGlossEn("man");
        l.setGender(LexemeGender.MASCULINE);
        l.setStatus(LexemeStatus.CANDIDATE);
        PartOfSpeech noun = new PartOfSpeech();
        noun.setCode("noun");
        noun.setGroup(PosGroup.NOMINAL);
        l.setPartsOfSpeech(Set.of(noun));
        return l;
    }

    private LexemeAdminService service(Lexeme lexeme) {
        LexemeRepository lexemeRepo = mock(LexemeRepository.class);
        when(lexemeRepo.findById(lexeme.getId())).thenReturn(Optional.of(lexeme));
        when(lexemeRepo.save(lexeme)).thenReturn(lexeme);
        return new LexemeAdminService(
                lexemeRepo,
                mock(LexemeFrequencyRepository.class),
                mock(PartOfSpeechRepository.class),
                mock(MorphologyClassRepository.class),
                mock(SemanticTopicRepository.class),
                new TransliterationService());
    }

    @Test
    void changeStatus_toApproved_nominalWithoutGender_throws422() {
        Lexeme lexeme = nominee();
        lexeme.setGender(null);

        assertThatThrownBy(() -> service(lexeme).changeStatus(lexeme.getId(), LexemeStatus.APPROVED))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode().value())
                .isEqualTo(422);
    }

    @Test
    void changeStatus_toApproved_transliterationMismatch_throws422() {
        Lexeme lexeme = nominee();
        lexeme.setLemmaDevanagari("नरः");

        assertThatThrownBy(() -> service(lexeme).changeStatus(lexeme.getId(), LexemeStatus.APPROVED))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode().value())
                .isEqualTo(422);
    }

    @Test
    void changeStatus_validTransliterationWithGender_succeeds() {
        Lexeme lexeme = nominee();

        LexemeDetailDto result = service(lexeme).changeStatus(lexeme.getId(), LexemeStatus.APPROVED);

        assertThat(result.status()).isEqualTo(LexemeStatus.APPROVED);
    }

    @Test
    void changeStatus_toRejected_fromCandidate_succeeds() {
        Lexeme lexeme = nominee();

        LexemeDetailDto result = service(lexeme).changeStatus(lexeme.getId(), LexemeStatus.REJECTED);

        assertThat(result.status()).isEqualTo(LexemeStatus.REJECTED);
    }
}