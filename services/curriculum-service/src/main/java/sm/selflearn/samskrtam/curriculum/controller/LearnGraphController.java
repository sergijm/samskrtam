package sm.selflearn.samskrtam.curriculum.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sm.selflearn.samskrtam.curriculum.dto.LearnGraphResponse;
import sm.selflearn.samskrtam.curriculum.service.LearnGraphService;

/**
 * Learning map page — the dashboard curriculum view. Returns real curriculum
 * topics grouped into layers with (currently random) per-user progress.
 */
@RestController
@RequestMapping("/api/v2/curriculum/learn-graph")
@RequiredArgsConstructor
public class LearnGraphController {

    private final LearnGraphService learnGraphService;

    @GetMapping
    public LearnGraphResponse getLearnGraph() {
        return learnGraphService.getLearnGraph();
    }
}