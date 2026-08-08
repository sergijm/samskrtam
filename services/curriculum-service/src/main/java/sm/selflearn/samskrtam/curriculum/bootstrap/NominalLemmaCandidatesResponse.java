package sm.selflearn.samskrtam.curriculum.bootstrap;

import java.util.List;

/**
 * Ответ sangraha-эндпоинта кандидатов на импорт существительных.
 */
public record NominalLemmaCandidatesResponse(
        List<NominalLemmaCandidateDto> candidates
) {
}