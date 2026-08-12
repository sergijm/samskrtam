package sm.selflearn.samskrtam.curriculum.lexicon.service;

import org.junit.jupiter.api.Test;
import sm.selflearn.samskrtam.curriculum.lexicon.dto.PoolCriteria;
import sm.selflearn.samskrtam.curriculum.lexicon.model.Lexeme;
import sm.selflearn.samskrtam.curriculum.lexicon.model.LexemeGender;
import sm.selflearn.samskrtam.curriculum.lexicon.model.LexemeStatus;
import sm.selflearn.samskrtam.curriculum.lexicon.model.LexicalTopicBinding;
import sm.selflearn.samskrtam.curriculum.lexicon.model.LexicalTopicBindingId;
import sm.selflearn.samskrtam.curriculum.lexicon.model.PartOfSpeech;
import sm.selflearn.samskrtam.curriculum.lexicon.model.UserLexemeProgress;
import sm.selflearn.samskrtam.curriculum.lexicon.model.UserLexemeProgressId;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.LexemeFrequencyRepository;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.LexemeRepository;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.LexicalTopicBindingRepository;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.SourceOccurrenceRepository;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.UserCollectionItemRepository;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.UserLexemeProgressRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LexemePoolServiceTest {

    private Lexeme lexeme(UUID id, String posCode) {
        Lexeme l = new Lexeme();
        l.setId(id);
        l.setLemmaIast("lemma-" + id);
        l.setLemmaDevanagari("ल");
        l.setLemmaSlp1("lemma-" + id);
        l.setGlossRu("глосс");
        l.setGlossEn("gloss");
        l.setGender(LexemeGender.MASCULINE);
        l.setStatus(LexemeStatus.APPROVED);
        PartOfSpeech pos = new PartOfSpeech();
        pos.setCode(posCode);
        l.setPartsOfSpeech(Set.of(pos));
        return l;
    }

    @Test
    void resolve_neverReturnsNonApproved() {
        LexemeRepository lexemeRepo = mock(LexemeRepository.class);
        Lexeme approved = lexeme(UUID.randomUUID(), "noun");
        when(lexemeRepo.findByStatus(LexemeStatus.APPROVED)).thenReturn(List.of(approved));
        when(lexemeRepo.findWithDetailsByIdIn(any())).thenReturn(List.of(approved));

        LexemePoolService service = poolService(lexemeRepo, mock(LexemeFrequencyRepository.class),
                mock(LexicalTopicBindingRepository.class), mock(SourceOccurrenceRepository.class),
                mock(UserCollectionItemRepository.class), mock(UserLexemeProgressRepository.class));

        assertThat(service.resolve(null)).hasSize(1);
    }

    @Test
    void excludeMasteredForUserId_filtersOnlyMasteredOfThatUser() {
        UUID userId = UUID.randomUUID();
        UUID lexemeMasteredId = UUID.randomUUID();
        UUID lexemeNewId = UUID.randomUUID();

        LexemeRepository lexemeRepo = mock(LexemeRepository.class);
        Lexeme mastered = lexeme(lexemeMasteredId, "noun");
        Lexeme fresh = lexeme(lexemeNewId, "noun");
        when(lexemeRepo.findByStatus(LexemeStatus.APPROVED)).thenReturn(List.of(mastered, fresh));
        when(lexemeRepo.findWithDetailsByIdIn(any()))
                .thenAnswer(inv -> inv.<List<UUID>>getArgument(0).stream()
                        .map(id -> id.equals(lexemeMasteredId) ? mastered : fresh)
                        .toList());

        UserLexemeProgress masteredProgress = new UserLexemeProgress();
        masteredProgress.setId(new UserLexemeProgressId());
        masteredProgress.getId().setUserId(userId);
        masteredProgress.getId().setLexemeId(lexemeMasteredId);
        masteredProgress.setMasteryScore((short) 95);

        UserLexemeProgressRepository progressRepo = mock(UserLexemeProgressRepository.class);
        when(progressRepo.findByIdUserId(userId)).thenReturn(List.of(masteredProgress));

LexemePoolService service = poolService(lexemeRepo, mock(LexemeFrequencyRepository.class),
                mock(LexicalTopicBindingRepository.class), mock(SourceOccurrenceRepository.class),
                mock(UserCollectionItemRepository.class), progressRepo);

        PoolCriteria criteria = new PoolCriteria(List.of(), null, null, List.of(), List.of(),
                null, null, null, userId, 100);
        var result = service.resolve(criteria);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(lexemeNewId);
    }

    @Test
    void reshuffle_keepsAtMostTwoConsecutivePos() {
        UUID id = UUID.randomUUID();
        Lexeme l1 = lexeme(id, "noun");
        Lexeme l2 = lexeme(UUID.randomUUID(), "noun");
        Lexeme l3 = lexeme(UUID.randomUUID(), "noun");
        Lexeme other = lexeme(UUID.randomUUID(), "verb");
        List<Lexeme> shuffled = LexemePoolService.reshuffleNoConsecutivePos(List.of(l1, l2, l3, other));
        assertThat(codeRunLength(shuffled)).isLessThanOrEqualTo(2);
    }

    @Test
    void resolve_twoTopics_neitherExceedsQuota() {
        UUID topicA = UUID.randomUUID();
        UUID topicB = UUID.randomUUID();
        Lexeme a1 = lexeme(UUID.randomUUID(), "noun");
        Lexeme a2 = lexeme(UUID.randomUUID(), "noun");
        Lexeme a3 = lexeme(UUID.randomUUID(), "noun");
        Lexeme b1 = lexeme(UUID.randomUUID(), "verb");
        Lexeme b2 = lexeme(UUID.randomUUID(), "verb");

        LexemeRepository lexemeRepo = mock(LexemeRepository.class);
        when(lexemeRepo.findByStatus(LexemeStatus.APPROVED))
                .thenReturn(List.of(a1, a2, a3, b1, b2));
        when(lexemeRepo.findWithDetailsByIdIn(any()))
                .thenAnswer(inv -> inv.<List<UUID>>getArgument(0).stream()
                        .map(id -> List.of(a1, a2, a3, b1, b2).stream()
                                .filter(l -> l.getId().equals(id)).findFirst().orElseThrow())
                        .toList());

        LexicalTopicBindingRepository bindingRepo = mock(LexicalTopicBindingRepository.class);
        when(bindingRepo.findByIdLexicalTopicId(topicA))
                .thenReturn(bindings(topicA, a1, a2, a3));
        when(bindingRepo.findByIdLexicalTopicId(topicB))
                .thenReturn(bindings(topicB, b1, b2));

        LexemePoolService service = poolService(lexemeRepo, mock(LexemeFrequencyRepository.class),
                bindingRepo, mock(SourceOccurrenceRepository.class),
                mock(UserCollectionItemRepository.class), mock(UserLexemeProgressRepository.class));

        // poolLimit=6, 2 темы → квота = ceil(6/2)+2 = 5 (не лимитирует в этом случае).
        // Проверяем, что обе темы представлены и квота уважается при лимите 3.
        PoolCriteria tight = new PoolCriteria(List.of(topicA, topicB), null, null,
                List.of(), List.of(), null, null, null, null, 3);
        var result = service.resolve(tight);

        int fromA = (int) result.stream().filter(c -> c.posCode().equals("noun")).count();
        int fromB = (int) result.stream().filter(c -> c.posCode().equals("verb")).count();
        assertThat(fromA).isLessThanOrEqualTo((int) Math.ceil(3.0 / 2) + 2);
        assertThat(fromB).isLessThanOrEqualTo((int) Math.ceil(3.0 / 2) + 2);
        assertThat(result).isNotEmpty();
    }

    private List<LexicalTopicBinding> bindings(UUID topicId, Lexeme... lexemes) {
        return java.util.Arrays.stream(lexemes).map(l -> {
            LexicalTopicBinding b = new LexicalTopicBinding();
            LexicalTopicBindingId id = new LexicalTopicBindingId();
            id.setLexicalTopicId(topicId);
            id.setLexemeId(l.getId());
            b.setId(id);
            return b;
        }).toList();
    }

    private int codeRunLength(List<Lexeme> lexemes) {
        if (lexemes.isEmpty()) {
            return 0;
        }
        String first = lexemes.get(0).getPartsOfSpeech().iterator().next().getCode();
        int run = 1;
        for (int i = 1; i < lexemes.size(); i++) {
            String code = lexemes.get(i).getPartsOfSpeech().iterator().next().getCode();
            if (code.equals(first)) {
                run++;
            } else {
                break;
            }
        }
        return run;
    }

    private LexemePoolService poolService(
            LexemeRepository lexemeRepo,
            LexemeFrequencyRepository freqRepo,
            LexicalTopicBindingRepository bindingRepo,
            SourceOccurrenceRepository occurrenceRepo,
            UserCollectionItemRepository collectionRepo,
            UserLexemeProgressRepository progressRepo) {
        return new LexemePoolService(lexemeRepo, freqRepo, bindingRepo, occurrenceRepo,
                collectionRepo, progressRepo);
    }
}