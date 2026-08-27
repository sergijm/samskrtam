package sm.selflearn.samskrtam.samcli.io;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqlScriptRunnerTest {

    @Test
    void splitsOnSemicolonsIgnoringComments() {
        String script = """
                -- leading comment
                /* block
                   comment */
                INSERT INTO lingua.lemmas (a) VALUES (1);
                INSERT INTO lingua.lemmas (b) VALUES (2);  -- trailing comment
                INSERT INTO lingua.lemmas (c) VALUES (3)
                """;

        List<String> stmts = SqlScriptRunner.splitStatements(script);

        assertEquals(3, stmts.size());
        assertTrue(stmts.get(0).startsWith("INSERT INTO lingua.lemmas (a)"));
        assertTrue(stmts.get(1).startsWith("INSERT INTO lingua.lemmas (b)"));
        assertTrue(stmts.get(2).startsWith("INSERT INTO lingua.lemmas (c)"));
        assertTrue(stmts.get(2).endsWith("VALUES (3)"));
    }

    @Test
    void treatsSemicolonInsideBlockCommentAsContent() {
        String script = "/* a ; b */ INSERT INTO t VALUES (1);";
        List<String> stmts = SqlScriptRunner.splitStatements(script);
        assertEquals(1, stmts.size());
        assertTrue(stmts.get(0).startsWith("INSERT INTO t VALUES (1)"));
        assertTrue(stmts.get(0).contains("a ; b") == false);
    }
}
