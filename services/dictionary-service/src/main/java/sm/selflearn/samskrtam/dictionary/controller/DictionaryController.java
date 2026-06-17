package sm.selflearn.samskrtam.dictionary.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sm.selflearn.samskrtam.dictionary.dto.MwEntryDto;
import sm.selflearn.samskrtam.monierwilliams.dto.MwWordSearchDto;
import sm.selflearn.samskrtam.dictionary.service.DictionaryService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dictionary")
@RequiredArgsConstructor
public class DictionaryController {

    private final DictionaryService dictionaryService;

    @GetMapping("/search")
    public List<MwWordSearchDto> searchWords(@RequestParam String query) {
        return dictionaryService.searchWords(query);
    }

    @GetMapping("/entry")
    public ResponseEntity<MwEntryDto> getEntry(@RequestParam String slp1Spelling) {
        return ResponseEntity.ok().body(dictionaryService.getEntryBySlp1Spelling(slp1Spelling));
    }
}
