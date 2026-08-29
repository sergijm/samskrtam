package sm.selflearn.samskrtam.samcli.importing;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import sm.selflearn.samskrtam.samcli.cae.CaeEntry;
import sm.selflearn.samskrtam.samcli.cae.CaeGrammarInfo;
import sm.selflearn.samskrtam.samcli.io.CaeFileParser;
import sm.selflearn.samskrtam.samcli.io.IdentifierValidator;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Импортёр словаря Каппеллера (cae.txt) — аналог {@link MwImporter} для mw.
 * Создаёт плоскую целевую таблицу и пакетно вставляет распарсенные записи.
 */
public class CaeImporter {

    private static final Logger log = LoggerFactory.getLogger(CaeImporter.class);

    private static final ObjectMapper GRAMMAR_MAPPER = new ObjectMapper();

    private final JdbcTemplate jdbcTemplate;

    public CaeImporter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void prepareSchema(String schema, String table) {
        String s = IdentifierValidator.requireValid(schema);
        String t = IdentifierValidator.requireValid(table);
        jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS \"" + s + "\"");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS \"" + s + "\".\"" + t + "\" (" +
                "id BIGSERIAL PRIMARY KEY, " +
                "cae_id BIGINT NOT NULL, " +
                "page INTEGER NOT NULL, " +
                "homonym_num SMALLINT, " +
                "entry_variant TEXT, " +
                "headword_plain TEXT NOT NULL, " +
                "headword_accented TEXT NOT NULL, " +
                "raw_text TEXT, " +
                "clean_text TEXT, " +
                "gloss TEXT, " +
                "grammar JSONB NOT NULL DEFAULT '{}', " +
                "foreign_refs JSONB, " +
                "compound_forms JSONB, " +
                "imported_at TIMESTAMPTZ NOT NULL DEFAULT now(), " +
                "CONSTRAINT " + t + "_cae_id_uniq UNIQUE (cae_id))");
        log.info("Prepared schema \"{}\" and table \"{}\".", s, t);
    }

    public int importFile(Path file, String schema, String table, int batchSize,
                          boolean truncate) {
        String s = IdentifierValidator.requireValid(schema);
        String t = IdentifierValidator.requireValid(table);

        if (truncate) {
            jdbcTemplate.update("TRUNCATE TABLE \"" + s + "\".\"" + t + "\"");
            log.info("Truncated \"{}.\"{}.", s, t);
        }

        String sql = "INSERT INTO \"" + s + "\".\"" + t + "\" " +
                "(cae_id, page, homonym_num, entry_variant, headword_plain, headword_accented, " +
                "raw_text, clean_text, gloss, grammar, foreign_refs, compound_forms) VALUES " +
                "(?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?::jsonb, ?::jsonb) " +
                "ON CONFLICT (cae_id) DO UPDATE SET " +
                "page = EXCLUDED.page, " +
                "homonym_num = EXCLUDED.homonym_num, " +
                "entry_variant = EXCLUDED.entry_variant, " +
                "headword_plain = EXCLUDED.headword_plain, " +
                "headword_accented = EXCLUDED.headword_accented, " +
                "raw_text = EXCLUDED.raw_text, " +
                "clean_text = EXCLUDED.clean_text, " +
                "gloss = EXCLUDED.gloss, " +
                "grammar = EXCLUDED.grammar, " +
                "foreign_refs = EXCLUDED.foreign_refs, " +
                "compound_forms = EXCLUDED.compound_forms, " +
                "imported_at = now()";

        final List<CaeEntry> batch = new ArrayList<>(Math.max(1, batchSize));
        final int[] imported = {0};
        final int[] lastLogged = {0};

        try {
            CaeFileParser.parse(file, entry -> {
                batch.add(entry);
                if (batch.size() >= batchSize) {
                    flush(sql, batch);
                    imported[0] += batch.size();
                    batch.clear();
                    if (imported[0] - lastLogged[0] >= 10_000) {
                        log.info("Imported {} entries...", imported[0]);
                        lastLogged[0] = imported[0];
                    }
                }
            });
            if (!batch.isEmpty()) {
                flush(sql, batch);
                imported[0] += batch.size();
                batch.clear();
            }
        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to read cae.txt: " + file, e);
        }

        log.info("Flushed all batches. Total imported: {}", imported[0]);
        return imported[0];
    }

    private void flush(String sql, List<CaeEntry> batch) {
        jdbcTemplate.batchUpdate(sql, batch, batch.size(), (ps, entry) -> {
            CaeGrammarInfo g = entry.getGrammar();
            ps.setLong(1, entry.getId());
            ps.setInt(2, entry.getPage());
            if (entry.getHomonymNum() != null) {
                ps.setShort(3, entry.getHomonymNum().shortValue());
            } else {
                ps.setNull(3, java.sql.Types.SMALLINT);
            }
            ps.setString(4, entry.getEntryVariant());
            ps.setString(5, entry.getHeadwordPlain());
            ps.setString(6, entry.getHeadwordAccented());
            ps.setString(7, entry.getRawText());
            ps.setString(8, entry.getCleanText());
            ps.setString(9, entry.getGloss());
            ps.setString(10, serializeGrammar(g));
            ps.setString(11, serializeForeignRefs(g));
            ps.setString(12, serializeCompoundForms(g));
        });
    }

    private String serializeGrammar(CaeGrammarInfo grammar) {
        try {
            return GRAMMAR_MAPPER.writeValueAsString(grammar);
        } catch (Exception e) {
            log.warn("Failed to serialize grammar for an entry, storing empty object: {}", e.toString());
            return "{}";
        }
    }

    private String serializeForeignRefs(CaeGrammarInfo grammar) {
        try {
            return GRAMMAR_MAPPER.writeValueAsString(grammar.getForeignRefs());
        } catch (Exception e) {
            log.warn("Failed to serialize foreignRefs for an entry: {}", e.toString());
            return "[]";
        }
    }

    private String serializeCompoundForms(CaeGrammarInfo grammar) {
        try {
            return GRAMMAR_MAPPER.writeValueAsString(grammar.getCompoundForms());
        } catch (Exception e) {
            log.warn("Failed to serialize compoundForms for an entry: {}", e.toString());
            return "[]";
        }
    }
}
