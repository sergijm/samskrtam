package sm.selflearn.samskrtam.curriculum.lexicon.imports;

import java.util.List;
import java.util.UUID;

/**
 * One lemma row exported by sangraha-service (lemmas/export). Reused by the
 * verse-batch request payload ({@code VerseLemmaBatchRequest.words}).
 */
public record LemmaExportItem(
        UUID id,
        String lemmaSlp1,
        String lemmaIast,
        String lemmaDevanagari,
        String gender,
        String dominantPosCode,
        int occurrences,
        List<String> categoryCodes,
        String glossRu,
        String glossEn,
        String vowelType) {
}
