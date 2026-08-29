package sm.selflearn.samskrtam.sangraha.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sm.selflearn.samskrtam.sangraha.dto.ChapterSummaryDto;
import sm.selflearn.samskrtam.sangraha.dto.CreateChapterRequest;
import sm.selflearn.samskrtam.sangraha.dto.CreateVerseRequest;
import sm.selflearn.samskrtam.sangraha.dto.CreateWorkRequest;
import sm.selflearn.samskrtam.sangraha.dto.UpdateChapterRequest;
import sm.selflearn.samskrtam.sangraha.dto.UpdateWorkRequest;
import sm.selflearn.samskrtam.sangraha.dto.VerseTreeDto;
import sm.selflearn.samskrtam.sangraha.dto.WorkSummaryDto;
import sm.selflearn.samskrtam.sangraha.service.ChapterService;
import sm.selflearn.samskrtam.sangraha.service.VerseService;
import sm.selflearn.samskrtam.sangraha.service.WorkService;

import java.util.UUID;

/**
 * Write-контур Sangraha (произведения → главы → стихи). Весь контур — ADMIN
 * (sangraha-service.md §4). Фактическая проверка роли выполняется на API Gateway
 * (IdentityHeader + роль из токена Keycloak); сервис доверяет проброшенному
 * заголовку и не дублирует SecurityFilterChain.
 */
@RestController
@RequestMapping("/api/v1/sangraha")
@RequiredArgsConstructor
public class SangrahaWriteController {

    private final WorkService workService;
    private final ChapterService chapterService;
    private final VerseService verseService;

    @PostMapping("/works")
    public ResponseEntity<WorkSummaryDto> createWork(@RequestBody CreateWorkRequest request) {
        return ResponseEntity.ok(WorkService.toSummary(workService.createWork(request)));
    }

    @PutMapping("/works/{workSlug}")
    public ResponseEntity<WorkSummaryDto> updateWork(
            @PathVariable String workSlug,
            @RequestBody UpdateWorkRequest request) {
        return ResponseEntity.ok(WorkService.toSummary(workService.updateWork(workSlug, request)));
    }

    @PostMapping("/works/{workSlug}/chapters")
    public ResponseEntity<ChapterSummaryDto> createChapter(
            @PathVariable String workSlug,
            @RequestBody CreateChapterRequest request) {
        UUID workId = workService.getWorkBySlug(workSlug).getId();
        var chapter = chapterService.createChapter(workId, request);
        return ResponseEntity.ok(toChapterSummary(chapter.getId()));
    }

    @PutMapping("/chapters/{chapterId}")
    public ResponseEntity<ChapterSummaryDto> updateChapter(
            @PathVariable UUID chapterId,
            @RequestBody UpdateChapterRequest request) {
        var chapter = chapterService.updateChapter(chapterId, request);
        return ResponseEntity.ok(toChapterSummary(chapter.getId()));
    }

    @PostMapping("/chapters/{chapterId}/verses")
    public ResponseEntity<VerseTreeDto> createVerse(
            @PathVariable UUID chapterId,
            @RequestBody CreateVerseRequest request) {
        return ResponseEntity.ok(verseService.createVerse(chapterId, request));
    }

    private ChapterSummaryDto toChapterSummary(UUID chapterId) {
        var chapter = chapterService.getChapterById(chapterId);
        int verseCount = chapterService.getChapterVersesByChapterId(chapterId).verses().size();
        return ChapterService.toSummary(chapter, verseCount);
    }
}
