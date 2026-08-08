package sm.selflearn.samskrtam.curriculum.lexicon.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sm.selflearn.samskrtam.curriculum.lexicon.dto.LexemeProgressDto;
import sm.selflearn.samskrtam.curriculum.lexicon.dto.LexemeProgressUpdateRequest;
import sm.selflearn.samskrtam.curriculum.lexicon.service.LexemeProgressService;

import java.util.List;
import java.util.UUID;

/**
 * Прогресс освоения лексем пользователем (task-curriculum-15 §8/§9).
 * GET  /api/v2/lexicon/users/{userId}/progress?lexemeIds=...
 * PATCH /api/v2/lexicon/users/{userId}/progress/{lexemeId} {correct: boolean}
 */
@RestController
@RequestMapping("/api/v2/lexicon/users/{userId}/progress")
@RequiredArgsConstructor
public class LexemeProgressController {

    private final LexemeProgressService progressService;

    @GetMapping
    public List<LexemeProgressDto> getProgress(
            @PathVariable UUID userId,
            @RequestParam List<UUID> lexemeIds) {
        return progressService.getProgress(userId, lexemeIds);
    }

    @PatchMapping("/{lexemeId}")
    public LexemeProgressDto recordAnswer(
            @PathVariable UUID userId,
            @PathVariable UUID lexemeId,
            @RequestBody LexemeProgressUpdateRequest request) {
        return progressService.recordAnswer(userId, lexemeId, request.correct());
    }
}