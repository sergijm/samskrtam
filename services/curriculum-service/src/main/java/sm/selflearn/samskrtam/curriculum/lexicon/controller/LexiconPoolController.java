package sm.selflearn.samskrtam.curriculum.lexicon.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sm.selflearn.samskrtam.curriculum.lexicon.dto.LexemeCandidateDto;
import sm.selflearn.samskrtam.curriculum.lexicon.dto.PoolCriteria;
import sm.selflearn.samskrtam.curriculum.lexicon.service.LexemePoolService;

import java.util.List;
import java.util.UUID;

/**
 * GET /api/v2/lexicon/pool/resolve — единственная точка входа для quiz-service
 * при генерации lexical-сессий (task-curriculum-15 §7, lexical-quizzes.md §5).
 */
@RestController
@RequestMapping("/api/v2/lexicon/pool")
@RequiredArgsConstructor
public class LexiconPoolController {

    private final LexemePoolService lexemePoolService;

    @GetMapping("/resolve")
    public List<LexemeCandidateDto> resolve(
            @RequestParam(required = false) List<UUID> topicIds,
            @RequestParam(required = false) Integer frequencyRankMin,
            @RequestParam(required = false) Integer frequencyRankMax,
            @RequestParam(required = false) List<String> posCodes,
            @RequestParam(required = false) List<String> morphologyClassCodes,
            @RequestParam(required = false) UUID collectionId,
            @RequestParam(required = false) UUID excludeMasteredForUserId,
            @RequestParam(required = false) Integer poolLimit) {
        PoolCriteria criteria = new PoolCriteria(
                topicIds, frequencyRankMin, frequencyRankMax,
                posCodes, morphologyClassCodes,
                collectionId,
                excludeMasteredForUserId, poolLimit);
        return lexemePoolService.resolve(criteria);
    }
}
