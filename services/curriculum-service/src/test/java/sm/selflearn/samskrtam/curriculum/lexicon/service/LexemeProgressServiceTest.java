package sm.selflearn.samskrtam.curriculum.lexicon.service;

import org.junit.jupiter.api.Test;
import sm.selflearn.samskrtam.curriculum.lexicon.dto.LexemeProgressDto;
import sm.selflearn.samskrtam.curriculum.lexicon.model.Lexeme;
import sm.selflearn.samskrtam.curriculum.lexicon.model.UserLexemeProgress;
import sm.selflearn.samskrtam.curriculum.lexicon.model.UserLexemeProgressId;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.LexemeRepository;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.UserLexemeProgressRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LexemeProgressServiceTest {

    private UserLexemeProgressId id(UUID userId, UUID lexemeId) {
        UserLexemeProgressId id = new UserLexemeProgressId();
        id.setUserId(userId);
        id.setLexemeId(lexemeId);
        return id;
    }

    private UserLexemeProgress progress(UUID userId, UUID lexemeId, short score, int exposure) {
        UserLexemeProgress p = new UserLexemeProgress();
        p.setId(id(userId, lexemeId));
        p.setMasteryScore(score);
        p.setExposureCount(exposure);
        p.setCorrectCount(0);
        p.setIncorrectCount(0);
        return p;
    }

    @Test
    void recordAnswer_createsRowOnFirstCorrect() {
        UUID userId = UUID.randomUUID();
        UUID lexemeId = UUID.randomUUID();
        UserLexemeProgressRepository progressRepo = mock(UserLexemeProgressRepository.class);
        when(progressRepo.findById(id(userId, lexemeId))).thenReturn(Optional.empty());
        when(progressRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        LexemeRepository lexemeRepo = mock(LexemeRepository.class);
        when(lexemeRepo.findById(lexemeId)).thenReturn(Optional.of(new Lexeme()));

        LexemeProgressService service = new LexemeProgressService(progressRepo, lexemeRepo);

        LexemeProgressDto dto = service.recordAnswer(userId, lexemeId, true);

        assertThat(dto.exposureCount()).isEqualTo(1);
        assertThat(dto.correctCount()).isEqualTo(1);
        assertThat(dto.masteryScore()).isEqualTo((short) 50);
        assertThat(dto.nextReviewAt()).isNotNull();
    }

    @Test
    void recordAnswer_firstIncorrectScoresLow() {
        UUID userId = UUID.randomUUID();
        UUID lexemeId = UUID.randomUUID();
        UserLexemeProgressRepository progressRepo = mock(UserLexemeProgressRepository.class);
        when(progressRepo.findById(id(userId, lexemeId))).thenReturn(Optional.empty());
        when(progressRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        LexemeRepository lexemeRepo = mock(LexemeRepository.class);
        when(lexemeRepo.findById(lexemeId)).thenReturn(Optional.of(new Lexeme()));

        LexemeProgressService service = new LexemeProgressService(progressRepo, lexemeRepo);

        LexemeProgressDto dto = service.recordAnswer(userId, lexemeId, false);

        assertThat(dto.incorrectCount()).isEqualTo(1);
        assertThat(dto.masteryScore()).isLessThanOrEqualTo((short) 10);
    }

    @Test
    void recordAnswer_incrementsExistingRow() {
        UUID userId = UUID.randomUUID();
        UUID lexemeId = UUID.randomUUID();
        UserLexemeProgressRepository progressRepo = mock(UserLexemeProgressRepository.class);
        when(progressRepo.findById(id(userId, lexemeId)))
                .thenReturn(Optional.of(progress(userId, lexemeId, (short) 50, 3)));
        when(progressRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        LexemeRepository lexemeRepo = mock(LexemeRepository.class);

        LexemeProgressService service = new LexemeProgressService(progressRepo, lexemeRepo);

        LexemeProgressDto dto = service.recordAnswer(userId, lexemeId, true);

        assertThat(dto.exposureCount()).isEqualTo(4);
        assertThat(dto.masteryScore()).isEqualTo((short) 75);
    }

    @Test
    void getProgress_returnsOnlyExistingRows() {
        UUID userId = UUID.randomUUID();
        UUID presentId = UUID.randomUUID();
        UUID missingId = UUID.randomUUID();
        UserLexemeProgressRepository progressRepo = mock(UserLexemeProgressRepository.class);
        when(progressRepo.findById(id(userId, presentId)))
                .thenReturn(Optional.of(progress(userId, presentId, (short) 80, 5)));
        when(progressRepo.findById(id(userId, missingId))).thenReturn(Optional.empty());
        LexemeRepository lexemeRepo = mock(LexemeRepository.class);

        LexemeProgressService service = new LexemeProgressService(progressRepo, lexemeRepo);

        List<LexemeProgressDto> result = service.getProgress(userId, List.of(presentId, missingId));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).lexemeId()).isEqualTo(presentId);
    }
}