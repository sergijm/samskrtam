package sm.selflearn.samskrtam.sangraha.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import sm.selflearn.samskrtam.sangraha.dto.DeclensionExamplesSearchRequestDto;
import sm.selflearn.samskrtam.sangraha.dto.DeclensionExamplesSearchResponseDto;
import sm.selflearn.samskrtam.sangraha.dto.NominalLemmaCandidatesResponseDto;
import sm.selflearn.samskrtam.sangraha.dto.VerseWordExportPageDto;
import sm.selflearn.samskrtam.sangraha.dto.VersesBatchRequestDto;
import sm.selflearn.samskrtam.sangraha.dto.VersesBatchResponseDto;
import sm.selflearn.samskrtam.sangraha.service.NominalLemmaCandidateService;
import sm.selflearn.samskrtam.sangraha.service.VerseBatchService;
import sm.selflearn.samskrtam.sangraha.service.VerseWordExportService;
import sm.selflearn.samskrtam.sangraha.service.VerseWordSearchService;

import java.util.UUID;

/**
 * Internal service-to-service endpoints для content-service (§9 sangraha-service.md):
 * - POST /sangraha/internal/content/declension-examples — примеры склонений
 * - POST /sangraha/internal/content/verses/batch — пакетный запрос стихов
 * - GET  /sangraha/internal/content/verse-words/export — экспорт слов корпуса
 * - GET  /sangraha/internal/content/nominal-lemmas — кандидаты на импорт
 * Не публичные, вызываются напрямую content-service по SANGRAHA_SERVICE_URL.
 */
@Slf4j
@RestController
@RequestMapping("/sangraha/internal/content")
@RequiredArgsConstructor
public class InternalContentController {

    private final VerseWordSearchService verseWordSearchService;
    private final VerseBatchService verseBatchService;
    private final NominalLemmaCandidateService nominalLemmaCandidateService;
    private final VerseWordExportService verseWordExportService;

    @PostMapping("/declension-examples")
    public ResponseEntity<DeclensionExamplesSearchResponseDto> searchDeclensionExamples(
            @RequestBody DeclensionExamplesSearchRequestDto request) {
        log.debug("Declension examples request: vowelType={}, gender={}, cells={}",
                request.vowelType(), request.gender(), request.cells().size());
        DeclensionExamplesSearchResponseDto response = verseWordSearchService.searchExamples(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/verse-words/export")
    public ResponseEntity<VerseWordExportPageDto> exportVerseWords(
            @RequestParam(required = false) UUID cursor,
            @RequestParam(defaultValue = "500") int limit) {
        log.debug("Verse word export request: cursor={}, limit={}", cursor, limit);
        VerseWordExportPageDto page = verseWordExportService.export(cursor, Math.min(Math.max(limit, 1), 500));
        return ResponseEntity.ok(page);
    }

    @PostMapping("/verses/batch")
    public ResponseEntity<VersesBatchResponseDto> fetchVersesBatch(
            @RequestBody VersesBatchRequestDto request) {
        log.debug("Verses batch request: {} ids", request.verseIds().size());
        VersesBatchResponseDto response = verseBatchService.fetchBatch(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/nominal-lemmas")
    public ResponseEntity<NominalLemmaCandidatesResponseDto> fetchNominalLemmaCandidates(
            @RequestParam(required = false) String stemClass,
            @RequestParam(defaultValue = "100") int limit) {
        log.debug("Nominal lemma candidates request: stemClass={}, limit={}", stemClass, limit);
        NominalLemmaCandidatesResponseDto response =
                nominalLemmaCandidateService.findCandidates(stemClass, limit);
        return ResponseEntity.ok(response);
    }
}