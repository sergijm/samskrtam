package sm.selflearn.samskrtam.curriculum.questsession.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import sm.selflearn.samskrtam.curriculum.questsession.dto.QuizSessionComposeRequest;
import sm.selflearn.samskrtam.curriculum.questsession.dto.QuizSessionComposeResponse;
import sm.selflearn.samskrtam.curriculum.questsession.service.QuizSessionComposerService;

/**
 * Composition of a quiz session question sequence (API v2). Contract-first,
 * see docs/services/curriculum-service/curriculum-session-composition.md.
 */
@RestController
@RequestMapping("/api/v2/curriculum/sessions")
@RequiredArgsConstructor
@Tag(name = "Quiz Session Composition", description = "Builds a quiz session question sequence from topics")
public class QuizSessionComposerController {

    private final QuizSessionComposerService composerService;

    /**
     * Compose a random-ordered sequence of ready-made questions: the caller specifies
     * topics and a question count per topic (mixed grammar + lexical topics allowed);
     * curriculum-service returns the sequence with options/distractors already
     * materialized.
     */
    @PostMapping("/compose")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Compose a quiz session sequence from topics",
            description = "Returns ready-made questions (with distractors) in random order spanning the requested topics")
    @ApiResponse(responseCode = "200", description = "Composed sequence")
    @ApiResponse(responseCode = "400", description = "Empty topics or a topic with no materialized items")
    @ApiResponse(responseCode = "404", description = "Unknown topic code")
    public QuizSessionComposeResponse compose(@RequestBody QuizSessionComposeRequest request) {
        return composerService.compose(request);
    }
}