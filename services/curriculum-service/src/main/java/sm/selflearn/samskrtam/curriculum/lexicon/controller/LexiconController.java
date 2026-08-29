package sm.selflearn.samskrtam.curriculum.lexicon.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sm.selflearn.samskrtam.curriculum.lexicon.dto.LexiconDashboardResponse;
import sm.selflearn.samskrtam.curriculum.lexicon.service.LexiconDashboardService;

/**
 * Лендинг раздела «Лексика» (новая модель: lemma_translation +
 * lemma_lexical_topic). Без привязки к {@code Lexeme}.
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
