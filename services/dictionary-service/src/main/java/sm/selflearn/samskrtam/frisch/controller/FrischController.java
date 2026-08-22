package sm.selflearn.samskrtam.frisch.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sm.selflearn.samskrtam.content.dto.frisch.FrischEntryDto;
import sm.selflearn.samskrtam.frisch.service.FrischService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dictionary/frisch")
@RequiredArgsConstructor
public class FrischController {

    private final FrischService frischService;

    @GetMapping
    public ResponseEntity<List<FrischEntryDto>> getLemma(@RequestParam String lemma) {
        return ResponseEntity.ok(frischService.getLemma(lemma));
    }
}
