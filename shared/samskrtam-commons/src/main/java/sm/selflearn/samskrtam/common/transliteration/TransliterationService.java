package sm.selflearn.samskrtam.common.transliteration;

import com.wellebee.sanskrit.Sanscript;

import java.util.Optional;

/**
 * Сервис транслитерации санскрита. Единая реализация, вынесенная из
 * dictionary-service / curriculum-service / sangraha-service (раньше копировалась
 * по трём сервисам). Использует Sanscript.java для конвертации между схемами
 * (IAST, SLP1, Devanagari, ITRANS, HK).
 *
 * Статический, не содержит состояния — регистрируется как Spring-бин через
 * {@link TransliterationAutoConfiguration} (auto-configuration commons-модуля).
 */
public class TransliterationService {

    private final Sanscript sanscript = new Sanscript();

    /**
     * Нормализует пользовательский ввод в SLP1.
     */
    public String normalizeToSlp1(String input, String inputScheme) {
        // Если пользователь не указал схему, пробуем определить автоматически
        if (inputScheme == null || inputScheme.isEmpty()) {
            inputScheme = detectScheme(input);
        }

        try {
            var query = sanscript.t(input, inputScheme, "slp1");
            return slp1RemoveStress(query);
        } catch (Exception e) {
            // Если транслитерация не удалась, возвращаем оригинал
            return input;
        }
    }

    /**
     * Удаляет всё, кроме латинских букв (снимает ударение/знаки препинания в SLP1).
     */
    public String slp1RemoveStress(String input) {
        return Optional.ofNullable(input).map(
            it -> it.replaceAll("[^a-zA-Z]", "")
        ).orElse(null);
    }

    /**
     * Пытается определить схему ввода по содержимому.
     */
    private String detectScheme(String input) {
        if (input == null) return "iast";
        // Проверяем наличие символов деванагари
        if (input.matches(".*[\\u0900-\\u097F].*")) {
            return "devanagari";
        }
        // Проверяем наличие диакритиков (IAST)
        if (input.matches(".*[āīūṛṝḷḹṃḥśṣṭḍṇñ].*")) {
            return "iast";
        }
        // По умолчанию считаем, что это ITRANS или HK
        return "iast";
    }

    /**
     * Конвертация из SLP1 в IAST.
     */
    public String slp1ToIast(String slp1) {
        try {
            return sanscript.t(slp1, "slp1", "iast");
        } catch (Exception e) {
            return slp1;
        }
    }

    /**
     * Конвертация из IAST в SLP1.
     */
    public String iastToSlp1(String iast) {
        if (iast == null || iast.isBlank()) {
            return "";
        }
        try {
            return sanscript.t(iast, "iast", "slp1");
        } catch (Exception e) {
            return iast;
        }
    }

    /**
     * Конвертация из SLP1 в Devanagari.
     */
    public String slp1ToDevanagari(String slp1) {
        try {
            return sanscript.t(slp1, "slp1", "devanagari");
        } catch (Exception e) {
            return slp1;
        }
    }

    /**
     * Конвертация из IAST в Devanagari.
     */
    public String iastToDevanagari(String iast) {
        if (iast == null || iast.isBlank()) {
            return iast;
        }
        try {
            return sanscript.t(iast, "iast", "devanagari");
        } catch (Exception e) {
            return iast;
        }
    }

    /**
     * Конвертация из Devanagari в IAST.
     * Используется перед отправкой текста в LLM, чтобы модель всегда получала IAST
     * (промпт запрещает передавать и возвращать деванагари).
     */
    public String devanagariToIast(String devanagari) {
        if (devanagari == null || devanagari.isBlank()) return "";
        try {
            return sanscript.t(devanagari, "devanagari", "iast");
        } catch (Exception e) {
            return devanagari;
        }
    }

    /**
     * Детерминированная транслитерация IAST → SLP1, приведённая к виду slug
     * (только a-z, 0-9, дефисы). Используется для генерации slug произведений/глав.
     */
    public String iastToSlug(String iast) {
        if (iast == null || iast.isBlank()) {
            return "untitled";
        }
        try {
            String slp1 = sanscript.t(iast, "iast", "slp1");
            String slug = slp1
                .toLowerCase()
                .replaceAll("[\\s_]+", "-")
                .replaceAll("[^a-z0-9-]", "")
                .replaceAll("-{2,}", "-")
                .replaceAll("^-|-$", "");
            if (slug.isEmpty()) slug = "untitled";
            if (!slug.matches("^[a-z0-9].*")) slug = "w-" + slug;
            return slug;
        } catch (Exception e) {
            // fallback: убираем всё, кроме a-z, 0-9, пробелов
            String fallback = iast.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", "")
                .trim()
                .replaceAll("\\s+", "-");
            if (fallback.isEmpty()) fallback = "untitled";
            return fallback;
        }
    }

    /**
     * Определяет письменность текста по Unicode-диапазону.
     * @return "devanagari", если есть символы \u0900-\u097F, иначе "iast"
     */
    public String detectScript(String text) {
        if (text == null || text.isBlank()) return "iast";
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= '\u0900' && c <= '\u097F') {
                return "devanagari";
            }
        }
        return "iast";
    }

    /**
     * Детекция языка по Unicode-диапазону первого значимого символа.
     * @return "SANSKRIT", "RU" или "EN"
     */
    public String detectLanguage(String text) {
        if (text == null || text.isBlank()) return "EN";
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isLetter(c)) {
                if (c >= '\u0900' && c <= '\u097F') return "SANSKRIT";
                if ((c >= '\u0400' && c <= '\u04FF') || (c >= '\u0500' && c <= '\u052F')) return "RU";
                if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')) return "EN";
            }
        }
        return "EN";
    }
}
