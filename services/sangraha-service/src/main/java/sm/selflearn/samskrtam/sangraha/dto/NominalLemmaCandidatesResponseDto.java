package sm.selflearn.samskrtam.sangraha.dto;

import java.util.List;

/**
 * Ответ эндпоинта кандидатов на импорт существительных (для бутстрапа
 * склонений в curriculum-service). Члены — это {@link NominalLemmaCandidateDto}.
 */
public record NominalLemmaCandidatesResponseDto(
        List<NominalLemmaCandidateDto> candidates
) {
}