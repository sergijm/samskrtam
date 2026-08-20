package sm.selflearn.samskrtam.curriculum.lexicon.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sm.selflearn.samskrtam.curriculum.lexicon.dto.LexemeDetailDto;
import sm.selflearn.samskrtam.curriculum.lexicon.service.LexemeAdminService;

import java.util.List;
import java.util.UUID;

/**
 * Идемпотентная замена наборов таксономий лексемы (task-curriculum-16 §5, ADMIN).
 */
@RestController
@RequestMapping("/api/v2/lexicon/lexemes/{id}")
@RequiredArgsConstructor
public class LexemeTaxonomyController {

    private final LexemeAdminService adminService;

    @PutMapping("/semantic-classes")
    public LexemeDetailDto replaceSemanticClasses(@PathVariable UUID id,
                                                  @RequestBody List<UUID> classIds) {
        return adminService.replaceSemanticClasses(id, classIds);
    }

    @PutMapping("/pos")
    public LexemeDetailDto replacePos(@PathVariable UUID id, @RequestBody List<String> posCodes) {
        return adminService.replacePos(id, posCodes);
    }

    @PutMapping("/morphology")
    public LexemeDetailDto replaceMorphology(@PathVariable UUID id, @RequestBody List<String> codes) {
        return adminService.replaceMorphology(id, codes);
    }
}