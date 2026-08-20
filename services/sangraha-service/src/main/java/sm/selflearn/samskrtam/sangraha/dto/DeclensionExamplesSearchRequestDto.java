package sm.selflearn.samskrtam.sangraha.dto;

import sm.selflearn.samskrtam.content.model.CaseType;
import sm.selflearn.samskrtam.content.model.Gender;
import sm.selflearn.samskrtam.content.model.NumberType;
import sm.selflearn.samskrtam.content.model.VowelType;

/**
 * Запрос примеров словоформ по словоизменительному классу (sangraha-service.md §9).
 * {@code gender}/{@code caseType}/{@code numberType} опциональны: не заполнено — фильтр
 * по этому значению не применяется (стихи с любым родом/падежом/числом; один запрос
 * вместо списка ячеек). {@code gender} = UNKNOWN — осознанно матчит пусто (в корпусе
 * таких значений нет).
 */
public record DeclensionExamplesSearchRequestDto(
        VowelType vowelType,
        Gender gender,
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