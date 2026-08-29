package sm.selflearn.samskrtam.samcli.io;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import sm.selflearn.samskrtam.samcli.model.MwEntry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MwFileParserTest {

    @Test
    void parsesHeaderFieldsAndRawBody(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("mw.txt");
        String content = String.join("\n",
                "<L>1<pc>1,1<k1>a<k2>a<e>1",
                "<hom>1.</hom> <s>a</s> ¦ the first letter of the alphabet",
                "<LEND>",
                "<L>3<pc>1,1<k1>a<k2>a<h>2<e>1",
                "<hom>2.</hom> <s>a</s> ¦ a vocative particle",
                "<LEND>",
                "<L>4.7<pc>1,1<k1>a<k2>a<e>1A",
                "¦ occasionally denoting comparison",
                "<LEND>");
        Files.writeString(file, content, StandardCharsets.UTF_8);

        List<MwEntry> entries = new ArrayList<>();
        MwFileParser.parse(file, entries::add);

        assertEquals(3, entries.size());

        MwEntry first = entries.get(0);
        assertEquals("1", first.getEntryId());
        assertEquals("1,1", first.getPageCol());
        assertEquals("a", first.getKey1());
        assertEquals("a", first.getKey2());
        assertEquals(null, first.getHomonym());
        assertEquals("1", first.getEntryNo());
        assertTrue(first.getBody().contains("the first letter of the alphabet"));

        MwEntry second = entries.get(1);
        assertEquals("3", second.getEntryId());
        assertEquals("2", second.getHomonym());
        assertTrue(second.getBody().contains("a vocative particle"));

        MwEntry third = entries.get(2);
        assertEquals("4.7", third.getEntryId());
        assertEquals("1A", third.getEntryNo());
        assertTrue(third.getBody().contains("occasionally denoting comparison"));
    }

    @Test
    void ignoresLeadingNonEntryLines(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("mw.txt");
        String content = String.join("\n",
                "# header comment",
                "<L>1<pc>1,1<k1>a<k2>a<e>1",
                "<s>a</s> ¦ x",
                "<LEND>");
        Files.writeString(file, content, StandardCharsets.UTF_8);

        List<MwEntry> entries = new ArrayList<>();
        MwFileParser.parse(file, entries::add);

        assertEquals(1, entries.size());
        assertEquals("1", entries.get(0).getEntryId());
    }
}
