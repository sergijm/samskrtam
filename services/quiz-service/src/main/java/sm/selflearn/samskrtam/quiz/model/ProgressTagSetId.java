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
    NEW(false),
    LEARNING(false),
    MASTERED(false),
    DIFFICULT(false),
    SINGULAR(false),
    DUAL(false),
    PLURAL(false),
    NOMINATIVE(true),
    ACCUSATIVE(true),
    INSTRUMENTAL(true),
    DATIVE(true),
    ABLATIVE(true),
    GENITIVE(true),
    LOCATIVE(true),
    VOCATIVE(true),
    ACC_LOC(true),
    INS_ABL(true),
    GEN_LOC(true),
    DAT_ACC(true),
    GEN_ABL(true),
    INS_LOC(true),
    DAT_GEN(true),
    ABL_LOC(true),
    NOM_ACC(true);

    private final boolean isCase;

    ProgressTagSetId(boolean isCase) {
        this.isCase = isCase;
    }

    public boolean isCase() {
        return isCase;
    }
}
