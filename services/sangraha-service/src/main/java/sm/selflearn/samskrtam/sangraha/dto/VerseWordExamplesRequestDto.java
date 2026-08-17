package sm.selflearn.samskrtam.sangraha.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Запрос на поиск примера стиха по списку словоформ (surfaceIast) для колонки
 * «примеры из санграхи» в таблице слов урока склонений. Для каждой формы
 * возвращается ровно один самый короткий стих (3–7 слов, с глаголом).
 *
 * @param surfaceIasts уникальные поверхностные формы (IAST), например ["devaḥ", "devam"]
 */
public record VerseWordExamplesRequestDto(
        List<String> surfaceIasts
) {
    public VerseWordExamplesRequestDto {
        if (surfaceIasts == null) {
            surfaceIasts = new ArrayList<>();
        }
    }
}
