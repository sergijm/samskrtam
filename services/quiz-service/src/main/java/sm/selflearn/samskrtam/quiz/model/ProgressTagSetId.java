package sm.selflearn.samskrtam.quiz.model;

/**
 * Стабильный идентификатор именованного прогресс-сета (ProgressTagSet).
 * Используется параметром {@code start}/@{code start-or-resume} для запуска/резюма
 * сессии по срезу прогресса урока (см. quest-engine.md §2.4, quiz-declension.md §3.4).
 *
 * <p>Статусные сеты существуют для любого урока; грамматические — только для уроков
 * склонений (DECLENSION_FORM). {@code DIFFICULT} — ортогональная ось к статусам.
 */
public enum ProgressTagSetId {
    NEW,
    LEARNING,
    MASTERED,
    DIFFICULT,
    SINGULAR,
    DUAL,
    PLURAL,
    NOMINATIVE,
    ACCUSATIVE,
    INSTRUMENTAL,
    DATIVE,
    ABLATIVE,
    GENITIVE,
    LOCATIVE,
    VOCATIVE,
    ACC_LOC,
    INS_ABL,
    GEN_LOC,
    DAT_ACC,
    GEN_ABL,
    INS_LOC,
    DAT_GEN,
    ABL_LOC
}
