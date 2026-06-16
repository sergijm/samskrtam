package sm.selflearn.samskrtam.monierwilliams.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import sm.selflearn.samskrtam.monierwilliams.entity.MwEntry;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

@Service
@Slf4j
public class MwTranslationExtractor {

    // Паттерн для извлечения основного перевода
    private static final Pattern TRANSLATION_PATTERN = Pattern.compile(
            "<body>\\s*" +                                    // начало body
                    "<s>[^<]+</s>\\s*" +                              // санскритское слово
                    "<lex>[^<]+</lex>\\s*" +                          // грамматическая информация
                    "(?:\\([^)]*\\)\\s*)?" +                          // этимология в скобках (опционально)
                    "([A-Za-z][^<]*?)\\.\\s*" +                       // перевод до точки
                    "(?:<ls>|$)"                                      // конец (источник или конец строки)
    );

    // Альтернативный паттерн (если нет этимологии)
    private static final Pattern TRANSLATION_PATTERN_SIMPLE = Pattern.compile(
            "<body>\\s*" +
                    "<s>[^<]+</s>\\s*" +
                    "<lex>[^<]+</lex>\\s*" +
                    "([A-Za-z][^<]*?)\\.\\s*" +
                    "(?:<ls>|$)"
    );

    // Паттерн для подзаписей (1B, 1C, 2, 3)
    private static final Pattern SUB_ENTRY_PATTERN = Pattern.compile(
            "<body>\\s*" +
                    "<s>[^<]+</s>\\s*" +
                    "(?:<lex>[^<]+</lex>\\s*)?" +
                    "([A-Za-z][^<]*?)(?:\\.\\s*<ls>|\\.\\s*$|,\\s*<ls>|$)"
    );

    /**
     * Извлекает основной перевод из записи
     */
    public String extractTranslation(MwEntry entry) {
        String body = entry.getBody();
        if (body == null || body.isEmpty()) {
            return "";
        }

        String eCode = entry.getECode();
        String translation = null;

        // Для основной записи (1, 1A)
        if ("1".equals(eCode) || "1A".equals(eCode)) {
            translation = extractMainTranslation(body);
        }

        // Для подзаписей (1B, 1C, 2, 3, 4)
        if (translation == null || translation.isEmpty()) {
            translation = extractSubTranslation(body);
        }

        // Если ничего не нашли, пробуем универсальный способ
        if (translation == null || translation.isEmpty()) {
            translation = extractGenericTranslation(body);
        }

        return translation != null ? translation.trim() : "";
    }

    /**
     * Извлечение из основной записи (с этимологией)
     */
    private String extractMainTranslation(String body) {
        // Пробуем с этимологией
        Matcher m = TRANSLATION_PATTERN.matcher(body);
        if (m.find()) {
            return m.group(1).trim();
        }

        // Пробуем без этимологии
        m = TRANSLATION_PATTERN_SIMPLE.matcher(body);
        if (m.find()) {
            return m.group(1).trim();
        }

        return null;
    }

    /**
     * Извлечение из подзаписи (без этимологии)
     */
    private String extractSubTranslation(String body) {
        Matcher m = SUB_ENTRY_PATTERN.matcher(body);
        if (m.find()) {
            return m.group(1).trim();
        }
        return null;
    }

    /**
     * Универсальный способ: удаляем все XML-теги и берём первое предложение
     */
    private String extractGenericTranslation(String body) {
        // Удаляем все XML-теги
        String text = body.replaceAll("<[^>]+>", " ");
        // Схлопываем пробелы
        text = text.replaceAll("\\s+", " ").trim();

        // Убираем санскритское слово в начале (если есть)
        text = text.replaceAll("^[a-zA-Z/]+\\s+", "");

        // Убираем грамматику (m., f., n., mfn., indec. и т.д.)
        text = text.replaceAll("^[mfn]\\.\\s+|^mfn\\.\\s+|^indec\\.\\s+", "");
        text = text.replaceAll("^mf\\([^)]+\\)n\\.\\s+", "");

        // Берём первое предложение (до точки)
        if (text.contains(".")) {
            int dotPos = text.indexOf(".");
            return text.substring(0, dotPos + 1).trim();
        }

        // Если точки нет, берём первые 100 символов
        if (text.length() > 10) {
            return text.substring(0, Math.min(100, text.length()));
        }

        return text;
    }
}