package sm.selflearn.samskrtam.curriculum.lexicon.imports;

/**
 * Итог одного batch-импорта лексики из корпуса sangraha-service
 * (lexicon-content-pipeline.md §2 шаг 6, task-curriculum-14 §E).
 */
public record SangrahaImportResult(
        int importedCount,
        int updatedCount,
        int totalLexemeCount
) {
}
