package sm.selflearn.samskrtam.curriculum.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sm.selflearn.samskrtam.curriculum.dto.SandhiRulesResponse;
import sm.selflearn.samskrtam.curriculum.service.SandhiRuleService;

@RestController
@RequestMapping("/api/v2/curriculum")
@RequiredArgsConstructor
@Slf4j
public class SandhiRuleController {

    private final SandhiRuleService sandhiRuleService;

    @GetMapping("/sandhi-rules")
    public ResponseEntity<SandhiRulesResponse> getAllSandhiRules() {
        log.info("GET /sandhi-rules (all)");
        return ResponseEntity.ok(sandhiRuleService.getAllRules());
    }

    @GetMapping("/sandhi-rules/{topicCode}")
    public ResponseEntity<SandhiRulesResponse> getSandhiRules(@PathVariable String topicCode) {
        log.info("GET /sandhi-rules/{}", topicCode);
        return ResponseEntity.ok(sandhiRuleService.getRulesForTopic(topicCode));
    }
}