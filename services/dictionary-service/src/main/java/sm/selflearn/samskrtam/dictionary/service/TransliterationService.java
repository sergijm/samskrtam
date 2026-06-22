package sm.selflearn.samskrtam.dictionary.service;

import com.wellebee.sanskrit.Sanscript;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class TransliterationService {

    private final Sanscript sanscript = new Sanscript();

    /**
     * Нормализует пользовательский ввод в SLP1
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
     * удаляем ударение в SLP1
     */
    public String slp1RemoveStress(String input) {

        return Optional.ofNullable(input).map(
            it->it.replaceAll("[^a-zA-Z]", "")
        ).orElse(null);
    }


    /**
     * Пытается определить схему ввода по содержимому
     */
    private String detectScheme(String input) {
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
     * Конвертация из SLP1 в IAST
     */
    public String slp1ToIast(String slp1) {
        // TODO: реализовать транслитерацию
        return slp1;
    }

    /**
     * Конвертация из IAST в SLP1
     */
    public String iastToSlp1(String iast) {
        // TODO: реализовать транслитерацию
        return iast;
    }

    /**
     * Конвертация из SLP1 в Devanagari
     */
    public String slp1ToDevanagari(String slp1) {
        // TODO: реализовать транслитерацию
        return slp1;
    }
}