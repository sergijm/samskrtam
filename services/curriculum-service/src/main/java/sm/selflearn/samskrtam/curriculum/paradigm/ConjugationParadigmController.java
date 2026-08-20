package sm.selflearn.samskrtam.curriculum.paradigm;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sm.selflearn.samskrtam.content.dto.ConjugationParadigmPageDto;
import sm.selflearn.samskrtam.content.model.Voice;

/**
 * v2 conjugation-paradigm page (present-tense carousel). Index-based:
 * {@code GET /api/v2/curriculum/topics/{topicCode}/conjugation-paradigms?index=N&voice=PARASMAIPADA}.
 */
@RestController
@RequestMapping("/api/v2/curriculum/topics/{topicCode}/conjugation-paradigms")
@RequiredArgsConstructor
public class ConjugationParadigmController {

    private final ConjugationParadigmService conjugationParadigmService;

    @GetMapping
    public ConjugationParadigmPageDto getParadigmPage(
            @PathVariable String topicCode,
            @RequestParam(defaultValue = "0") int index,
            @RequestParam(required = false) Voice voice) {
        return conjugationParadigmService.getParadigmPage(topicCode, index, voice);
    }
}