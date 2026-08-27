package sm.selflearn.samskrtam.sangraha.dto;

import sm.selflearn.samskrtam.content.model.CaseType;
import sm.selflearn.samskrtam.morphology.NumberType;
import sm.selflearn.samskrtam.content.model.VowelType;

/**
 * Запрос примеров словоформ по словоизменительному классу (sangraha-service.md §9).
 * {@code caseType}/{@code numberType} опциональны: не заполнено — фильтр
 * по этому значению не применяется (стихи с любым падежом/числом; один запрос
 * вместо списка ячеек).
 */
public record DeclensionExamplesSearchRequestDto(
        VowelType vowelType,
        CaseType caseType,
        NumberType numberType,
        int limitPerGroup,
        int maxPhraseWords
) {
    public DeclensionExamplesSearchRequestDto {
        if (maxPhraseWords == 0) {
            maxPhraseWords = 10;
        }
    }
}