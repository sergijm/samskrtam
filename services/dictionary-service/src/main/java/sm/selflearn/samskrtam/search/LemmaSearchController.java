package sm.selflearn.samskrtam.search;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sm.selflearn.samskrtam.search.dto.LemmaSearchResult;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dictionary/search")
@RequiredArgsConstructor
public class LemmaSearchController {

    private final LemmaSearchService service;

    @GetMapping("/lemma")
    public ResponseEntity<List<LemmaSearchResult>> search(
            @RequestParam String query,
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(service.search(query, limit));
    }
}
