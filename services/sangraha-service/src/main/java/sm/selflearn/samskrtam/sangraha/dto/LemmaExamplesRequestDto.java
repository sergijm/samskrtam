package sm.selflearn.samskrtam.sangraha.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Запрос на поиск примеров стихов по леммам (словарным формам, lemmaIast) для
 * раскрываемых строк таблицы слов лексического урока. Для каждой леммы
 * возвращается до {@code limitPerLemma} стихов, содержащих эту лемму.
 *
 * @param lemmas        уникальные леммы (IAST), например ["deva", "bhū"]
 * @param limitPerLemma максимум стихов на лемму (по умолчанию 5)
 */
public record LemmaExamplesRequestDto(
        List<String> lemmas,
        Integer limitPerLemma
) {
    public LemmaExamplesRequestDto {
        if (lemmas == null) {
            lemmas = new ArrayList<>();
        }
        if (limitPerLemma == null) {
            limitPerLemma = 5;
        }
    }
}
