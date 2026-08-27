package sm.selflearn.samskrtam.mw.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sm.selflearn.samskrtam.mw.dto.MwEntryDto;
import sm.selflearn.samskrtam.mw.service.MwService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dictionary/mw")
@RequiredArgsConstructor
public class MwController {

    private final MwService mwService;

    @GetMapping
    public ResponseEntity<List<MwEntryDto>> getEntry(@RequestParam String lemma) {
        return ResponseEntity.ok(mwService.getEntriesByLemma(lemma));
    }
}
