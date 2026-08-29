package sm.selflearn.samskrtam.samcli.cae;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import sm.selflearn.samskrtam.dictionary.mw.DerivationType;
import sm.selflearn.samskrtam.morphology.Gender;
import sm.selflearn.samskrtam.morphology.PartOfSpeech;
import sm.selflearn.samskrtam.samcli.io.CaeFileParser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CaeFileParserTest {

    @Test
    void parsesHeaderFieldsAndGrammar(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("cae.txt");
        String content = String.join("\n",
                "<L>1<pc>001<k1>a<k2>a<h>1",
                "1 {#a#}¦ <ab>pron.</ab> stem of 3^d <ab>pers.</ab>",
                "<LEND>",
                "<L>3<pc>001<k1>aMSa<k2>a/MSa",
                "{#a/MSa#}¦ <lex>m.</lex> portion, share, part, party; <ab>N.</ab> of a god.",
                "<LEND>",
                "<L>5633<pc>087<k1>*undara<k2>*undara<e>firstalt",
                "{#*undara#}¦ <lex>m.</lex>  mouse, rat.",
                "<LEND>");
        Files.writeString(file, content, StandardCharsets.UTF_8);

        List<CaeEntry> entries = new ArrayList<>();
        CaeFileParser.parse(file, entries::add);

        assertEquals(3, entries.size());

        CaeEntry first = entries.get(0);
        assertEquals(1L, first.getId());
        assertEquals(1, first.getPage());
        assertEquals("a", first.getHeadwordPlain());
        assertEquals("a", first.getHeadwordAccented());
        assertEquals(1, first.getHomonymNum());
        assertTrue(first.getGrammar().getPartsOfSpeech().contains(PartOfSpeech.PRONOUN));

        CaeEntry second = entries.get(1);
        assertEquals(3L, second.getId());
        assertEquals("aMSa", second.getHeadwordPlain());
        assertTrue(second.getGrammar().getGenders().contains(Gender.MASCULINE));
        assertTrue(second.getGrammar().getPartsOfSpeech().contains(PartOfSpeech.NOUN));
        // "N." (Name, proper noun) is not mapped -> goes to unmapped abbreviations
        assertTrue(second.getGrammar().getUnmappedAbbreviations().contains("N."));

        CaeEntry third = entries.get(2);
        assertEquals(5633L, third.getId());
        assertEquals("firstalt", third.getEntryVariant());
    }

    @Test
    void extractsForeignRefsAndCompoundForms(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("cae.txt");
        String content = String.join("\n",
                "<L>2<pc>001<k1>a<k2>a<h>2",
                "2 {#a#}¦ {#an#} neg. prefix, corresponding to Gr. <lang n=\"greek\">ἀ ἀν</lang>, Lat. in, Germ. un.",
                "<LEND>");
        Files.writeString(file, content, StandardCharsets.UTF_8);

        List<CaeEntry> entries = new ArrayList<>();
        CaeFileParser.parse(file, entries::add);

        assertEquals(1, entries.size());
        CaeEntry e = entries.get(0);
        assertEquals(1, e.getGrammar().getForeignRefs().size());
        assertEquals("greek", e.getGrammar().getForeignRefs().get(0).lang);
        assertTrue(e.getCleanText().contains("neg. prefix"));
    }
}
