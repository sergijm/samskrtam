package sm.selflearn.samskrtam.curriculum.paradigm;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sm.selflearn.samskrtam.content.dto.DeclensionParadigmPageDto;

/**
 * v2 declension-paradigm page for suppletive (pronoun) stems. Index-based carousel:
 * {@code GET /api/v2/curriculum/topics/{topicCode}/declension-paradigms?index=N}.
 */
@RestController
@RequestMapping("/api/v2/curriculum/topics/{topicCode}/declension-paradigms")
@RequiredArgsConstructor
public class ParadigmController {

    private final ParadigmService paradigmService;

    @GetMapping
    public DeclensionParadigmPageDto getParadigmPage(
            @PathVariable String topicCode,
            @RequestParam(defaultValue = "0") int index) {
        return paradigmService.getParadigmPage(topicCode, index);
    }
}