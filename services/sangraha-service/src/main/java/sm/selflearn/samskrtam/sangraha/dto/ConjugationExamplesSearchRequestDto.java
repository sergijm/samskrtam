package sm.selflearn.samskrtam.sangraha.dto;

import sm.selflearn.samskrtam.sangraha.model.Mood;
import sm.selflearn.samskrtam.sangraha.model.Tense;

/**
 * Запрос примеров стихов по спряжению (вкладка «Примеры» урока спряжений).
 * {@code tense}/{@code mood} опциональны: не заполнено — фильтр не применяется.
 */
public record ConjugationExamplesSearchRequestDto(
        Tense tense,
        Mood mood,
        int limitPerGroup,
        int maxPhraseWords
) {
    public ConjugationExamplesSearchRequestDto {
        if (maxPhraseWords == 0) {
            maxPhraseWords = 10;
        }
    }
}