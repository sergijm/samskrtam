package sm.selflearn.samskrtam.curriculum.lexicon.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sm.selflearn.samskrtam.curriculum.lexicon.dto.LexiconDashboardResponse;
import sm.selflearn.samskrtam.curriculum.lexicon.service.LexiconDashboardService;

/**
 * Lexicon home page (the "Лексика" dashboard). Returns the real lexicon
 * taxonomy with (currently random) per-user progress.
 */
@RestController
@RequestMapping("/api/v2/curriculum/lexicon")
@RequiredArgsConstructor
public class LexiconController {

    private final LexiconDashboardService lexiconDashboardService;

    @GetMapping
    public LexiconDashboardResponse getDashboard() {
        return lexiconDashboardService.getDashboard();
    }
}