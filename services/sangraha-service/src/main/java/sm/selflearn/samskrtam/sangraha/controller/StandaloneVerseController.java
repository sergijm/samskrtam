package sm.selflearn.samskrtam.sangraha.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sm.selflearn.samskrtam.sangraha.dto.StandaloneVerseItemDto;
import sm.selflearn.samskrtam.sangraha.dto.VerseDetailDto;
import sm.selflearn.samskrtam.sangraha.model.Verse;
import sm.selflearn.samskrtam.sangraha.service.StandaloneVerseService;
import sm.selflearn.samskrtam.sangraha.service.VerseService;

import java.util.List;
import java.util.UUID;

/**
 * Standalone-стихи пользователя (страница /analysis): verse.chapter_id = null,
 * стих не привязан к произведению/главе. Доступ — любой авторизованный
 * (X-User-Id из IdentityHeaderFilter), стихи персональные (owner_id).
 */
@RestController
@RequestMapping("/api/v1/sangraha")
@RequiredArgsConstructor
public class StandaloneVerseController {

    private final StandaloneVerseService standaloneVerseService;
    private final VerseService verseService;

    /**
     * Создаёт новую запись в verses (chapter_id = null, owner_id = текущий
     * пользователь) и запускает LLM-анализ. Возвращает детали созданного стиха
     * (статус ANALYZING сразу после запуска анализа).
     */
    @PostMapping("/analysis")
    public ResponseEntity<VerseDetailDto> createAndAnalyze(
            @RequestBody(required = false) VerseTextRequest request,
            @RequestHeader("X-User-Id") UUID userId) {
        String text = request == null ? null : request.text();
        Verse verse = standaloneVerseService.createAndAnalyze(text, userId);
        return ResponseEntity.ok(verseService.getVerseDetail(verse.getId()));
    }

    /** Список standalone-стихов текущего пользователя, новые сверху. */
    @GetMapping("/analysis")
    public ResponseEntity<List<StandaloneVerseItemDto>> listStandalone(
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(standaloneVerseService.list(userId));
    }

    /** Мягкое удаление standalone-стиха (только владельцем). */
    @DeleteMapping("/analysis/{verseId}")
    public ResponseEntity<Void> deleteStandalone(
            @PathVariable UUID verseId,
            @RequestHeader("X-User-Id") UUID userId) {
        standaloneVerseService.delete(verseId, userId);
        return ResponseEntity.noContent().build();
    }
}
