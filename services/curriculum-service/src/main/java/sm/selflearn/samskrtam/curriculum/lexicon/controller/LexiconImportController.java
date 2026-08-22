package sm.selflearn.samskrtam.curriculum.lexicon.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sm.selflearn.samskrtam.curriculum.lexicon.imports.VerseBatchImportResult;
import sm.selflearn.samskrtam.curriculum.lexicon.imports.VerseLemmaBatchRequest;
import sm.selflearn.samskrtam.curriculum.lexicon.imports.VerseLexemeImportService;

/**
 * Инкрементальный приём пачек лемм одного стиха от sangraha-service
 * (lexicon-content-pipeline.md §7): upsert переводов с meaningNumber +
 * создание/обновление VERSE-урока главы.
 */
@RestController
@RequestMapping("/api/v2/lexicon/import")
@RequiredArgsConstructor
public class LexiconImportController {

    private final VerseLexemeImportService verseLexemeImportService;

    @PostMapping("/verse-batch")
    public VerseBatchImportResult importVerseBatch(@RequestBody VerseLemmaBatchRequest request) {
        return verseLexemeImportService.importVerseBatch(request);
    }
}
