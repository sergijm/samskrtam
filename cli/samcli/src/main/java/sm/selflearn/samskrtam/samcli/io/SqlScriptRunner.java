package sm.selflearn.samskrtam.samcli.io;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * Minimal SQL-script executor for samcli. Splits a script into individual
 * statements on top-level semicolons (ignoring semicolons inside {@code --}
 * line comments and {@code /* *\/} block comments) and executes each via a
 * single {@link JdbcTemplate#execute(String)}.
 *
 * <p>Intended for the idempotent index-rebuild scripts (e.g.
 * {@code lingua_index_lemmas.sql}); it does NOT support dollar-quoted
 * {@code $$ ... $$} blocks — those are not used by the bundled scripts.</p>
 */
public final class SqlScriptRunner {

    private static final Logger log = LoggerFactory.getLogger(SqlScriptRunner.class);

    private SqlScriptRunner() {
    }

    /**
     * @return number of statements executed
     */
    public static int run(JdbcTemplate jdbcTemplate, String script) {
        int executed = 0;
        for (String stmt : splitStatements(script)) {
            if (stmt.isBlank()) {
                continue;
            }
            jdbcTemplate.execute(stmt);
            executed++;
        }
        return executed;
    }

    static List<String> splitStatements(String script) {
        List<String> out = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inLineComment = false;
        int i = 0;
        while (i < script.length()) {
            char c = script.charAt(i);
            if (inLineComment) {
                if (c == '\n') {
                    inLineComment = false;
                    current.append(c);
                }
                i++;
                continue;
            }
            // /* ... */ block comment
            if (c == '/' && i + 1 < script.length() && script.charAt(i + 1) == '*') {
                int end = script.indexOf("*/", i + 2);
                if (end < 0) {
                    break; // unterminated comment -> stop
                }
                i = end + 2;
                continue;
            }
            // -- line comment
            if (c == '-' && i + 1 < script.length() && script.charAt(i + 1) == '-') {
                inLineComment = true;
                i += 2;
                continue;
            }
            if (c == ';') {
                out.add(current.toString().trim());
                current.setLength(0);
                i++;
                continue;
            }
            current.append(c);
            i++;
        }
        if (!current.toString().isBlank()) {
            out.add(current.toString().trim());
        }
        return out;
    }
}
