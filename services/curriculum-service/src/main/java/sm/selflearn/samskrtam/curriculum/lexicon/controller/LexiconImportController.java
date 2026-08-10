package sm.selflearn.samskrtam.curriculum.lexicon.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sm.selflearn.samskrtam.curriculum.lexicon.imports.LexiconImportService;
import sm.selflearn.samskrtam.curriculum.lexicon.imports.SangrahaImportResult;

/**
 * ADMIN-эндпоинт запуска batch-импорта лексики из sangraha-service.
 * POST /api/v2/lexicon/import/from-sangraha
 */
@RestController
@RequestMapping("/api/v2/lexicon/import")
@RequiredArgsConstructor
public class LexiconImportController {

    private final LexiconImportService lexiconImportService;

    @PostMapping("/from-sangraha")
    public SangrahaImportResult importFromSangraha() {
        return lexiconImportService.importFromSangraha();
    }
}