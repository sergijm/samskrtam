package sm.selflearn.samskrtam.samcli.grammar;

import sm.selflearn.samskrtam.dictionary.mw.DerivationType;
import sm.selflearn.samskrtam.dictionary.mw.GrammarInfo;
import sm.selflearn.samskrtam.dictionary.mw.MwBodyParser;
import sm.selflearn.samskrtam.dictionary.mw.VerbInfo;
import sm.selflearn.samskrtam.morphology.Gender;
import sm.selflearn.samskrtam.morphology.Mood;
import sm.selflearn.samskrtam.morphology.NumberType;
import sm.selflearn.samskrtam.morphology.PartOfSpeech;
import sm.selflearn.samskrtam.morphology.Person;
import sm.selflearn.samskrtam.morphology.Tense;
import sm.selflearn.samskrtam.morphology.Voice;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MwBodyParserTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void parsesLexSummaryAndVerbInfoIntoEnums() {
        String body = "<info lex=\"m:f#ikA:n\"/>" +
                "<info verb=\"1\" cp=\"1\" parse=\"pres ind act v3s\"/>" +
                "<s>a</s> ¦ to go";

        GrammarInfo g = new MwBodyParser().parse(body).grammar;

        assertEquals(3, g.getLexSummary().size());
        assertEquals(Gender.MASCULINE, g.getLexSummary().get(0).getGenderEnum());
        assertEquals(Gender.FEMININE, g.getLexSummary().get(1).getGenderEnum());
        assertEquals("ikA", g.getLexSummary().get(1).getStem());
        assertEquals(Gender.NEUTER, g.getLexSummary().get(2).getGenderEnum());

        assertNotNull(g.getVerbInfo());
        assertEquals(PartOfSpeech.VERB, g.getPartOfSpeech());
        assertEquals(Tense.PRESENT, g.getVerbInfo().getTense());
        assertEquals(Mood.INDICATIVE, g.getVerbInfo().getMood());
        assertEquals(Voice.ACTIVE, g.getVerbInfo().getVoice());
        assertEquals(Person.THIRD, g.getVerbInfo().getPerson());
        assertEquals(NumberType.SINGULAR, g.getVerbInfo().getNumber());
        assertEquals(DerivationType.SIMPLE_INFLECTION, g.getVerbInfo().getDerivationType());
    }

    @Test
    void denominalVerbMapsToDenominativeDerivation() {
        String body = "<info verb=\"nom\" cp=\"10\" parse=\"pres ind act v2d\"/>";
        VerbInfo vi = new MwBodyParser().parse(body).grammar.getVerbInfo();
        assertEquals(DerivationType.DENOMINATIVE, vi.getDerivationType());
        assertEquals(Person.SECOND, vi.getPerson());
        assertEquals(NumberType.DUAL, vi.getNumber());
    }

    @Test
    void serializesToJsonWithEnumNames() throws Exception {
        String body = "<info verb=\"1\" cp=\"1\" parse=\"fut mid v1p\"/>";
        GrammarInfo g = new MwBodyParser().parse(body).grammar;
        String json = mapper.writeValueAsString(g);

        assertTrue(json.contains("\"partOfSpeech\":\"VERB\""));
        assertTrue(json.contains("\"tense\":\"FUTURE\""));
        assertTrue(json.contains("\"voice\":\"MIDDLE\""));
        assertTrue(json.contains("\"person\":\"FIRST\""));
        assertTrue(json.contains("\"number\":\"PLURAL\""));
    }
}
