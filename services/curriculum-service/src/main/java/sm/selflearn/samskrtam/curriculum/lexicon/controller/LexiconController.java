package sm.selflearn.samskrtam.curriculum.lexicon.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sm.selflearn.samskrtam.curriculum.lexicon.dto.LexiconDashboardResponse;
import sm.selflearn.samskrtam.curriculum.lexicon.service.LexiconDashboardService;

import java.util.UUID;

/**
 * Lexicon home page (the "Лексика" dashboard). Returns the real lexicon
 * taxonomy and actual per-user progress from {@code user_lexeme_progress};
 * without {@code X-User-Id} (anonymous) per-user counters are zero.
 */
@RestController
@RequestMapping("/api/v2/curriculum/lexicon")
@RequiredArgsConstructor
public class LexiconController {

    private final LexiconDashboardService lexiconDashboardService;

    @GetMapping
    public LexiconDashboardResponse getDashboard(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        return lexiconDashboardService.getDashboard(userId);
    }
}