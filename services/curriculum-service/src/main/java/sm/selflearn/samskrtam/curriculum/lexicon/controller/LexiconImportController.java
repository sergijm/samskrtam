package sm.selflearn.samskrtam.curriculum.lexicon.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sm.selflearn.samskrtam.curriculum.lexicon.imports.LexiconImportService;
import sm.selflearn.samskrtam.curriculum.lexicon.imports.SangrahaImportResult;
import sm.selflearn.samskrtam.curriculum.lexicon.imports.VerseBatchImportResult;
import sm.selflearn.samskrtam.curriculum.lexicon.imports.VerseLemmaBatchRequest;
import sm.selflearn.samskrtam.curriculum.lexicon.imports.VerseLexemeImportService;

/**
 * Приём лексики из sangraha-service: ADMIN batch-импорт всего корпуса
 * (lexicon-content-pipeline.md §2) и инкрементальные пачки по стихам (§7).
 */
@RestController
@RequestMapping("/api/v2/lexicon/import")
@RequiredArgsConstructor
public class LexiconImportController {

    private final LexiconImportService lexiconImportService;
    private final VerseLexemeImportService verseLexemeImportService;

    @PostMapping("/from-sangraha")
    public SangrahaImportResult importFromSangraha() {
        return lexiconImportService.importFromSangraha();
    }

    /**
     * Пачка лемм одного стиха от sangraha-service (lexicon-content-pipeline.md §7):
     * upsert лексем с meaningNumber + создание/обновление VERSE-урока главы.
     */
    @PostMapping("/verse-batch")
    public VerseBatchImportResult importVerseBatch(@RequestBody VerseLemmaBatchRequest request) {
        return verseLexemeImportService.importVerseBatch(request);
    }
}