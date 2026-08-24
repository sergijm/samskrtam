package sm.selflearn.samskrtam.sangraha.service;

import com.wellebee.sanskrit.Sanscript;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Сервис транслитерации санскрита, скопирован из dictionary-service.
 * Использует Sanscript.java (компилированная библиотека c++) для конвертации между схемами.
 */
@Service
public class TransliterationService {

    private final Sanscript sanscript = new Sanscript();

    /**
     * Детерминированная транслитерация IAST → SLP1.
     * Результат приводится к виду, пригодному для slug: только a-z, 0-9, дефисы.
     */
    public String iastToSlug(String iast) {
        if (iast == null || iast.isBlank()) {
            return "untitled";
        }
        try {
            String slp1 = sanscript.t(iast, "iast", "slp1");
            // Приводим к формату slug: нижний регистр, пробелы → дефисы, удаляем не-ASCII
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
     * @return "devanagari" если есть символы \u0900-\u097F, иначе "iast"
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
     * Конвертация из IAST в SLP1
     */
    public String iastToSlp1(String iast) {
        if (iast == null || iast.isBlank()) return "";
        try {
            return sanscript.t(iast, "iast", "slp1");
        } catch (Exception e) {
            return iast;
        }
    }

    /**
     * Конвертация из SLP1 в IAST
     */
    public String slp1ToIast(String slp1) {
        try {
            return sanscript.t(slp1, "slp1", "iast");
        } catch (Exception e) {
            return slp1;
        }
    }

    /**
     * Конвертация из IAST в Devanagari
     */
    public String iastToDevanagari(String iast) {
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