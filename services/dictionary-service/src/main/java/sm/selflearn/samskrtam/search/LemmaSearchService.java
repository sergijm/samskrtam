package sm.selflearn.samskrtam.search;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import sm.selflearn.samskrtam.dictionary.service.TransliterationService;
import sm.selflearn.samskrtam.search.dto.LemmaSearchResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Fuzzy, dictionary-agnostic lemma disambiguation. Maps an imprecise Sanskrit
 * surface form (any script/diacritic noise) to the exact dictionary lemma(s)
 * across ALL registered dictionaries. Four ranked layers:
 *
 *   1. DCS surface form -> lemma -> lemma_bridge -> lemmas  (corpus-aware)
 *   2. nominal-ending stripping -> lemmas                 (only if layer 1 empty)
 *   3. direct exact match on lemmas.search_key
 *   4. direct trigram fallback on lemmas.search_key        (only if still empty)
 *
 * Results are deduplicated by entry id (best score wins) and ranked by score.
 */
@Service
@RequiredArgsConstructor
public class LemmaSearchService {

    private final LemmaSearchRepository repo;
    private final TransliterationService transliterationService;

    public List<LemmaSearchResult> search(String query, int limit) {
        String qKey = transliterationService.normalizeToSlp1(query, null).toLowerCase();

        Map<String, Group> groups = new LinkedHashMap<>();

        // --- DCS exact → bridge ---
        for (DcsHit hit : repo.findDcsExact(query)) {
            for (LemmaRow row : resolveBridge(hit.lemmaKey(), hit.lemma())) {
                addHit(groups, row, "dcs_exact->bridge", 0.9,
                        Map.of("dcsLemma", hit.lemma(), "dcsFrequency", hit.frequency()));
            }
        }

        // --- DCS trigram → bridge ---
        for (DcsTrgmHit h : repo.findDcsTrgm(query, 5)) {
            for (LemmaRow row : resolveBridge(h.lemmaKey(), h.lemma())) {
                addHit(groups, row, "dcs_trgm->bridge", 0.9 * h.score(),
                        Map.of("dcsLemma", h.lemma(), "dcsFrequency", h.frequency()));
            }
        }

        // --- Direct exact ---
        for (LemmaRow row : repo.findLemmasExact(qKey)) {
            addHit(groups, row, "direct_exact", 1.0, Map.of());
        }

        // --- Direct trigram ---
        for (LemmaTrgmHit h : repo.findLemmasTrgm(qKey, limit * 2)) {
            addHit(groups, h.row(), "direct_trgm", h.score(), Map.of());
        }

        // --- Direct Levenshtein (max distance 1) ---
        for (LemmaTrgmHit h : repo.findLemmasLevenshtein(qKey, limit * 2)) {
            addHit(groups, h.row(), "direct_levenshtein", 0.9, Map.of());
        }

        // --- Ending-stripping exact + trigram ---
        for (String candidateKey : generateLemmaCandidates(qKey, repo.findEndings())) {
            for (LemmaRow row : repo.findLemmasExact(candidateKey)) {
                addHit(groups, row, "ending_strip_exact", 0.85,
                        Map.of("strippedTo", candidateKey));
            }
            for (LemmaTrgmHit h : repo.findLemmasTrgm(candidateKey, 3)) {
                addHit(groups, h.row(), "ending_strip_trgm", 0.6 * h.score(),
                        Map.of("strippedTo", candidateKey));
            }
        }

        // --- Build & rank ---
        List<LemmaSearchResult> ranked = new ArrayList<>();
        for (Group g : groups.values()) {
            LemmaRow row = g.bestRow;
            Map<String, long[]> entries = new LinkedHashMap<>();
            for (var e : g.entries.entrySet()) {
                entries.put(e.getKey(), e.getValue().stream().mapToLong(Long::longValue).toArray());
            }
            ranked.add(LemmaSearchResult.builder()
                    .lemmaId(row.id())
                    .dictionaryCode(row.dictionaryCode())
                    .k1Slp1(row.k1Slp1())
                    .k2Original(row.k2Original())
                    .headwordDisplay(row.headwordDisplay())
                    .lemmaDevanagari(toDevanagari(row))
                    .k1Iast(toIast(row))
                    .path(g.bestPath)
                    .score(g.bestScore)
                    .notes(g.bestNotes)
                    .entries(entries)
                    .build());
        }
        ranked.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        return ranked.size() > limit ? ranked.subList(0, limit) : ranked;
    }

    /**
     * Accumulates a single matched lemma row into its iast/search_key group.
     * Rows sharing the same iast (identical search_key) merge into one result,
     * with entry ids accumulated per dictionary code (mw/apte/frisch).
     */
    private void addHit(Map<String, Group> groups, LemmaRow row, String path,
                        double score, Map<String, Object> notes) {
        String key = row.k1Slp1();
        if (key == null || key.isEmpty()) {
            key = row.k2Original();
        };
        if (key == null || key.isEmpty()) {
            return;
        };
        Group g = groups.computeIfAbsent(key, k -> new Group());
        g.entries.computeIfAbsent(row.dictionaryCode(), c -> new ArrayList<>())
                .add(row.externalEntryId());
        if (g.bestRow == null || g.bestScore < score) {
            g.bestRow = row;
            g.bestScore = score;
            g.bestPath = path;
            g.bestNotes = new LinkedHashMap<>(notes);
        }
    }

    private static final class Group {
        LemmaRow bestRow;
        double bestScore;
        String bestPath;
        Map<String, Object> bestNotes = new LinkedHashMap<>();
        final Map<String, List<Long>> entries = new LinkedHashMap<>();
    }

    private String toDevanagari(LemmaRow row) {
        if (row.k1Slp1() != null) {
            String d = transliterationService.slp1ToDevanagari(row.k1Slp1());
            if (d != null) return d;
        }
        if (row.k2Original() != null) {
            try {
                String slp1 = transliterationService.normalizeToSlp1(row.k2Original(), "iast");
                if (slp1 != null) {
                    return transliterationService.slp1ToDevanagari(slp1);
                }
            } catch (Exception ignored) {}
        }
        return row.k2Original();
    }

    private String toIast(LemmaRow row) {
        if (row.k1Slp1() != null) {
            String iast = transliterationService.slp1ToIast(row.k1Slp1());
            if (iast != null) return iast;
        }
        return row.k2Original();
    }

    /**
     * Resolves a DCS lemma key to unified lemma rows, caching the resolution in
     * lemma_bridge. Returns cached ids, or empty if cached as unresolved, or
     * resolves + caches on first sight.
     */
    private List<LemmaRow> resolveBridge(String lemmaKey, String lemmaRaw) {
        List<Long> cached = repo.findBridgeLemmaIds(lemmaKey);
        if (!cached.isEmpty()) {
            return repo.findLemmasByIds(cached);
        }
        if (repo.isUnresolved(lemmaKey)) {
            return List.of();
        }

        List<LemmaRow> exact = repo.findLemmasExact(lemmaKey);
        if (!exact.isEmpty()) {
            for (LemmaRow row : exact) {
                repo.insertBridge(lemmaKey, row.id(), "exact", 1.00);
            }
            return exact;
        }

        List<LemmaTrgmHit> trgm = repo.findLemmasTrgm(lemmaKey, 3);
        if (!trgm.isEmpty()) {
            List<LemmaRow> rows = new ArrayList<>();
            for (LemmaTrgmHit h : trgm) {
                repo.insertBridge(lemmaKey, h.row().id(), "trgm", h.score());
                rows.add(h.row());
            }
            return rows;
        }

        repo.insertUnresolved(lemmaKey, lemmaRaw);
        return List.of();
    }

    /**
     * Strips every matching known ending (longest first) from an already
     * normalized query key and reconstructs the candidate citation form.
     */
    static List<String> generateLemmaCandidates(String queryKey, List<EndingPair> endings) {
        Set<String> seen = new LinkedHashSet<>();
        for (EndingPair pair : endings) {
            String ending = pair.ending();
            if (ending == null || ending.isEmpty()) {
                continue;
            }
            if (queryKey.endsWith(ending.toLowerCase()) && queryKey.length() > ending.length()) {
                String stem = queryKey.substring(0, queryKey.length() - ending.length());
                seen.add(stem + pair.lemmaSuffix().toLowerCase());
            }
        }
        return new ArrayList<>(seen);
    }
}
