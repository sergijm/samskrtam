package sm.selflearn.samskrtam.samcli.importing;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import sm.selflearn.samskrtam.dictionary.mw.GrammarInfo;
import sm.selflearn.samskrtam.dictionary.mw.MwBodyParser;
import sm.selflearn.samskrtam.samcli.io.IdentifierValidator;
import sm.selflearn.samskrtam.samcli.io.MwFileParser;
import sm.selflearn.samskrtam.samcli.model.MwEntry;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class MwImporter {

    private static final Logger log = LoggerFactory.getLogger(MwImporter.class);

    private static final ObjectMapper GRAMMAR_MAPPER = new ObjectMapper();

    private final MwBodyParser bodyParser = new MwBodyParser();

    private final JdbcTemplate jdbcTemplate;

    public MwImporter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void prepareSchema(String schema, String table) {
        String s = IdentifierValidator.requireValid(schema);
        String t = IdentifierValidator.requireValid(table);
        jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS \"" + s + "\"");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS \"" + s + "\".\"" + t + "\" (" +
                "id BIGSERIAL PRIMARY KEY, " +
                "entry_id TEXT NOT NULL, " +
                "page_col TEXT, " +
                "key1 TEXT, " +
                "key2 TEXT, " +
                "homonym TEXT, " +
                "entry_no TEXT, " +
                "body TEXT, " +
                "grammar JSONB, " +
                "clean_text TEXT, " +
                "imported_at TIMESTAMPTZ NOT NULL DEFAULT now(), " +
                "CONSTRAINT " + t + "_entry_id_uniq UNIQUE (entry_id))");
        jdbcTemplate.execute("ALTER TABLE \"" + s + "\".\"" + t + "\" " +
                "ADD COLUMN IF NOT EXISTS grammar JSONB");
        jdbcTemplate.execute("ALTER TABLE \"" + s + "\".\"" + t + "\" " +
                "ADD COLUMN IF NOT EXISTS clean_text TEXT");
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
                "(entry_id, page_col, key1, key2, homonym, entry_no, body, grammar, clean_text) VALUES " +
                "(?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?) " +
                "ON CONFLICT (entry_id) DO UPDATE SET " +
                "page_col = EXCLUDED.page_col, " +
                "key1 = EXCLUDED.key1, " +
                "key2 = EXCLUDED.key2, " +
                "homonym = EXCLUDED.homonym, " +
                "entry_no = EXCLUDED.entry_no, " +
                "body = EXCLUDED.body, " +
                "grammar = EXCLUDED.grammar, " +
                "clean_text = EXCLUDED.clean_text, " +
                "imported_at = now()";

        final List<MwEntry> batch = new ArrayList<>(Math.max(1, batchSize));
        final int[] imported = {0};
        final int[] lastLogged = {0};

        try {
            MwFileParser.parse(file, entry -> {
                MwBodyParser.Result parsed = parseBody(entry.getBody());
                entry.setGrammarJson(serializeGrammar(parsed.grammar));
                entry.setCleanText(parsed.cleanText);
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
            throw new RuntimeException("Failed to read mw.txt: " + file, e);
        }

        log.info("Flushed all batches. Total imported: {}", imported[0]);
        return imported[0];
    }

    private void flush(String sql, List<MwEntry> batch) {
        jdbcTemplate.batchUpdate(sql, batch, batch.size(), (ps, entry) -> {
            ps.setString(1, entry.getEntryId());
            ps.setString(2, entry.getPageCol());
            ps.setString(3, entry.getKey1());
            ps.setString(4, entry.getKey2());
            ps.setString(5, entry.getHomonym());
            ps.setString(6, entry.getEntryNo());
            ps.setString(7, entry.getBody());
            ps.setString(8, entry.getGrammarJson());
            ps.setString(9, entry.getCleanText());
        });
    }

    private MwBodyParser.Result parseBody(String body) {
        try {
            return bodyParser.parse(body);
        } catch (Exception e) {
            log.warn("Failed to parse body for an entry, storing empty grammar/clean text: {}", e.toString());
            return new MwBodyParser.Result(new GrammarInfo(), "");
        }
    }

    private String serializeGrammar(GrammarInfo grammar) {
        try {
            return GRAMMAR_MAPPER.writeValueAsString(grammar);
        } catch (Exception e) {
            log.warn("Failed to serialize grammar for an entry, storing empty object: {}", e.toString());
            return "{}";
        }
    }
}
