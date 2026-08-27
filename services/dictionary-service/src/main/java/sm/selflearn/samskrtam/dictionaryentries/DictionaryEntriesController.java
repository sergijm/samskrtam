package sm.selflearn.samskrtam.dictionaryentries;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dictionary")
@RequiredArgsConstructor
public class DictionaryEntriesController {

    private final DictionaryEntriesService service;

    @GetMapping("/entries")
    public ResponseEntity<DictionaryEntriesResponse> getEntries(
            @RequestParam String dictionary,
            @RequestParam List<Long> entryIds) {
        return ResponseEntity.ok(service.getEntries(dictionary, entryIds));
    }
}
