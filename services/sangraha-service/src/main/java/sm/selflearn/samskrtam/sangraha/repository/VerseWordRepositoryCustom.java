package sm.selflearn.samskrtam.sangraha.repository;

import java.util.List;

/**
 * Кастомные (нативные, через {@link jakarta.persistence.EntityManager}) методы
 * {@link VerseWordRepository}, не выразимые декларативно через {@code @Query}.
 */
public interface VerseWordRepositoryCustom {

    /**
     * Ячейки парадигмы {@code (caseType, numberType)} со стихами-примерами: стихи, у
     * которых есть слово с набором (stemClass, gender, caseType, numberType) — точная
     * проверка через предвычисленное поле {@code tuples} в {@code grammar_info}
     * ({@code [stemClass, gender, case, number]} на слово), кортеж служит только фильтром.
     * {@code caseType}/{@code numberType} допускают {@code null} — пропущенное значение
     * означает «любой» по этой оси (в кортеж не включается). Ячейки парадигмы стиха берутся
     * из именованных полей {@code grammar_info.caseType}/{@code grammar_info.numberType}
     * (стих, покрывающий несколько ячеек, даёт несколько строк), SQL ранжирует по
     * (caseType, numberType) и внутри — по word_count, затем verse_id, обрезает до
     * limitPerGroup на ячейку через ROW_NUMBER.
     */
    List<VerseWordRepository.VerseCellCount> findDeclensionExampleCells(
            String gender,
            String caseType,
            String numberType,
            String vowelType,
            int maxPhraseWords,
            int limitPerGroup);


}