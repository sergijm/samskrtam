package sm.selflearn.samskrtam.sangraha.service;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Утилиты транслитерации/генерации slug для произведений и глав.
 *
 * <p>Полноценная таблица IAST↔SLP1 для slug (sangraha-service.md §8, открытый
 * вопрос) выбирается отдельно; здесь — прагматичный slugify: нормализация
 * Unicode (снятие диакритики), приведение к lower-case и замена всего, кроме
 * латинских букв/цифр, на дефисы. Гарантирует непустой ASCII-slug.</p>
 */
public final class SlugUtils {

    private SlugUtils() {
    }

    public static String slugify(String input) {
        if (input == null) {
            return "";
        }
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFKD);
        String ascii = normalized.replaceAll("\\p{M}", "");
        String lower = ascii.toLowerCase(Locale.ROOT);
        String dashed = lower.replaceAll("[^a-z0-9]+", "-");
        String trimmed = dashed.replaceAll("^-+|-+$", "");
        return trimmed;
    }

    /**
     * Делает slug уникальным, добавляя суффикс -2, -3 … пока {@code exists}
     * возвращает true для кандидата.
     */
    public static String uniqueSlug(String base, java.util.function.Predicate<String> exists) {
        String slug = slugify(base);
        if (slug.isEmpty()) {
            slug = "item";
        }
        if (!exists.test(slug)) {
            return slug;
        }
        int i = 2;
        while (exists.test(slug + "-" + i)) {
            i++;
        }
        return slug + "-" + i;
    }
}
