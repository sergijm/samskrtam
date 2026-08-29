package sm.selflearn.samskrtam.apte.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sm.selflearn.samskrtam.apte.dto.ApteEntryDto;
import sm.selflearn.samskrtam.apte.service.ApteService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dictionary/apte")
@RequiredArgsConstructor
public class ApteController {

    private final ApteService apteService;

    @GetMapping
    public ResponseEntity<List<ApteEntryDto>> getEntry(@RequestParam String lemma) {
        return ResponseEntity.ok(apteService.getEntriesByLemma(lemma));
    }
}
