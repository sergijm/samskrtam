package sm.selflearn.samskrtam.curriculum.lexicon.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sm.selflearn.samskrtam.curriculum.dto.CaseEndingDto;
import sm.selflearn.samskrtam.curriculum.dto.VerbalEndingDto;
import sm.selflearn.samskrtam.curriculum.lexicon.service.CaseEndingService;
import sm.selflearn.samskrtam.curriculum.lexicon.service.VerbalEndingService;

import java.util.List;

@RestController
@RequestMapping("/api/v2/curriculum/lingua")
@RequiredArgsConstructor
public class CaseEndingController {

    private final CaseEndingService caseEndingService;
    private final VerbalEndingService verbalEndingService;

    @GetMapping("/case-endings")
    public List<CaseEndingDto> getCaseEndings() {
        return caseEndingService.findAll();
    }

    @GetMapping("/verbal-endings")
    public List<VerbalEndingDto> getVerbalEndings() {
        return verbalEndingService.findAll();
    }
}
