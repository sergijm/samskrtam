package sm.selflearn.samskrtam.curriculum.lexicon.service;

import com.wellebee.sanskrit.Sanscript;
import org.springframework.stereotype.Service;

/**
 * Транслитерация санскрита IAST → SLP1 / Devanagari (Sanscript.java), копия
 * из sangraha-service. Используется для вычисления {@code lemmaSlp1} при импорте
 * лексики (lexicon-content-pipeline.md §3) и для сверки транслитерации в admin-CRUD.
 */
@Service
public class TransliterationService {

    private final Sanscript sanscript = new Sanscript();

    public String iastToSlp1(String iast) {
        if (iast == null || iast.isBlank()) {
            return iast;
        }
        try {
            return sanscript.t(iast, "iast", "slp1");
        } catch (Exception e) {
            return iast;
        }
    }

    public String slp1ToIast(String slp1) {
        if (slp1 == null || slp1.isBlank()) {
            return slp1;
        }
        try {
            return sanscript.t(slp1, "slp1", "iast");
        } catch (Exception e) {
            return slp1;
        }
    }

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
}