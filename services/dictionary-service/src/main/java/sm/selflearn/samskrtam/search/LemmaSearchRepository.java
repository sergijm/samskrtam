package sm.selflearn.samskrtam.search;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Array;
import java.sql.PreparedStatement;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class LemmaSearchRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final RowMapper<LemmaRow> LEMMA_ROW = (rs, rn) -> new LemmaRow(
            rs.getLong("id"),
            rs.getString("dictionary_code"),
            rs.getString("k1_slp1"),
            rs.getString("k2_original"),
            rs.getString("headword_display"),
            rs.getLong("external_entry_id"),
            rs.getString("search_key"));

    public List<LemmaRow> findLemmasExact(String key) {
        return jdbcTemplate.query(
                "SELECT id, dictionary_code, k1_slp1, k2_original, headword_display, "
                        + "external_entry_id, search_key "
                        + "FROM lingua.lemmas WHERE search_key = ?",
                LEMMA_ROW, key);
    }

    public List<LemmaTrgmHit> findLemmasTrgm(String key, int limit) {
        return jdbcTemplate.query(
                "SELECT id, dictionary_code, k1_slp1, k2_original, headword_display, "
                        + "external_entry_id, search_key, "
                        + "similarity(search_key, ?::text) AS score "
                        + "FROM lingua.lemmas "
                        + "WHERE similarity(search_key, ?::text)>0.4 "
                        + "ORDER BY score DESC LIMIT ?",
                (rs, rn) -> new LemmaTrgmHit(
                        new LemmaRow(rs.getLong("id"), rs.getString("dictionary_code"),
                                rs.getString("k1_slp1"), rs.getString("k2_original"),
                                rs.getString("headword_display"),
                                rs.getLong("external_entry_id"),
                                rs.getString("search_key")),
                        rs.getDouble("score")),
                key, key, limit);
    }

    /**
     * Fuzzy match by Levenshtein distance (max distance 1). Trigram prefilter
     * (similarity > 0.4) keeps the scan small; results are ordered by distance.
     * Score is fixed at 0.9 as requested by the caller.
     */
    public List<LemmaTrgmHit> findLemmasLevenshtein(String key, int limit) {
        return jdbcTemplate.query(
                "SELECT id, dictionary_code, k1_slp1, k2_original, headword_display, "
                        + "external_entry_id, search_key, "
                        + "lingua.levenshtein(search_key, ?::text) AS dist "
                        + "FROM lingua.lemmas "
                        + "WHERE similarity(search_key, ?::text) > 0.4 "
                        + "AND lingua.levenshtein(search_key, ?::text) <= 1 "
                        + "ORDER BY dist LIMIT ?",
                (rs, rn) -> new LemmaTrgmHit(
                        new LemmaRow(rs.getLong("id"), rs.getString("dictionary_code"),
                                rs.getString("k1_slp1"), rs.getString("k2_original"),
                                rs.getString("headword_display"),
                                rs.getLong("external_entry_id"),
                                rs.getString("search_key")),
                        0.9),
                key, key, key, limit);
    }

    public List<LemmaRow> findLemmasByIds(List<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return jdbcTemplate.query(
                "SELECT id, dictionary_code, k1_slp1, k2_original, headword_display, "
                        + "external_entry_id, search_key "
                        + "FROM lingua.lemmas WHERE id = ANY(?)",
                (PreparedStatement ps) -> {
                    Array arr = ps.getConnection().createArrayOf("bigint", ids.toArray());
                    ps.setArray(1, arr);
                },
                LEMMA_ROW);
    }

    public List<DcsHit> findDcsExact(String surfaceKey) {
        return jdbcTemplate.query(
                "SELECT DISTINCT lemma, lemma_key, frequency "
                        + "FROM lingua.dcs_surface_forms WHERE surface_key = ? "
                        + "ORDER BY frequency DESC LIMIT 10",
                (rs, rn) -> new DcsHit(rs.getString("lemma"), rs.getString("lemma_key"), rs.getInt("frequency")),
                surfaceKey);
    }

    public List<DcsTrgmHit> findDcsTrgm(String surfaceKey, int limit) {
        return jdbcTemplate.query(
                "SELECT DISTINCT surface_form, lemma, lemma_key, frequency, "
                        + "similarity(surface_key, ?::text) AS score "
                        + "FROM lingua.dcs_surface_forms WHERE surface_key % ?::text "
                        + "ORDER BY score DESC, frequency DESC LIMIT ?",
                (rs, rn) -> new DcsTrgmHit(rs.getString("surface_form"), rs.getString("lemma"),
                        rs.getString("lemma_key"), rs.getInt("frequency"), rs.getDouble("score")),
                surfaceKey, surfaceKey, limit);
    }

    public List<Long> findBridgeLemmaIds(String dcsLemmaKey) {
        return jdbcTemplate.query(
                "SELECT lemma_id FROM lingua.lemma_bridge WHERE dcs_lemma_key = ?",
                (rs, rn) -> rs.getLong("lemma_id"),
                dcsLemmaKey);
    }

    public boolean isUnresolved(String dcsLemmaKey) {
        return !jdbcTemplate.query(
                "SELECT 1 FROM lingua.lemma_bridge_unresolved WHERE dcs_lemma_key = ?",
                (rs, rn) -> rs.getInt(1),
                dcsLemmaKey).isEmpty();
    }

    public void insertBridge(String dcsLemmaKey, long lemmaId, String method, double confidence) {
        jdbcTemplate.update(
                "INSERT INTO lingua.lemma_bridge (dcs_lemma_key, lemma_id, match_method, confidence) "
                        + "VALUES (?, ?, ?, ?) ON CONFLICT (dcs_lemma_key, lemma_id) DO NOTHING",
                dcsLemmaKey, lemmaId, method, confidence);
    }

    public void insertUnresolved(String dcsLemmaKey, String dcsLemmaRaw) {
        jdbcTemplate.update(
                "INSERT INTO lingua.lemma_bridge_unresolved (dcs_lemma_key, dcs_lemma_raw, lookup_count) "
                        + "VALUES (?, ?, 1) ON CONFLICT (dcs_lemma_key) DO UPDATE SET "
                        + "lookup_count = lingua.lemma_bridge_unresolved.lookup_count + 1, last_seen_at = now()",
                dcsLemmaKey, dcsLemmaRaw);
    }

    public List<EndingPair> findEndings() {
        return jdbcTemplate.query(
                "SELECT ending, lemma_suffix FROM lingua.nominal_endings ORDER BY length(ending) DESC",
                (rs, rn) -> new EndingPair(rs.getString("ending"), rs.getString("lemma_suffix")));
    }
}
