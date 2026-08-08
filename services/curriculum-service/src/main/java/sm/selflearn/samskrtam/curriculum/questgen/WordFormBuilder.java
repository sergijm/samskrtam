package sm.selflearn.samskrtam.curriculum.questgen;

import java.util.Set;

/**
 * Composes a full inflected word form from a lexeme (lemma = stem) and a
 * paradigm ending, replicating the composition content-service relied on for
 * {@code content.declension_form}: when the ending starts with a vowel, the
 * stem's final vowel coalesces with it (the paradigm endings already carry the
 * resulting long/guna vowel), otherwise the ending is appended as-is.
 *
 * <p>Works on both the IAST and the Devanagari spellings. The Devanagari rule
 * mirrors the IAST one using the inherent-{@code a} and mātrā mechanics of the
 * script (dropping the final vowel of the last syllable before a vowel-initial
 * ending). No new sandhi rules are invented — the vowel coalescence is already
 * baked into the stored paradigm endings.
 */
final class WordFormBuilder {

    private WordFormBuilder() {
    }

    public record Form(String iast, String devanagari) {
    }

    static Form compose(String lemmaIast, String lemmaDevanagari,
                        String endingIast, String endingDevanagari) {
        if (startsWithVowelIast(endingIast)) {
            return new Form(dropFinalVowel(lemmaIast) + endingIast,
                    normalizeDevanagari(dropFinalVowelDevanagari(lemmaDevanagari) + endingDevanagari));
        }
        return new Form(lemmaIast + endingIast, lemmaDevanagari + endingDevanagari);
    }

    /**
     * True when the ending should be attached to a stem stripped of its final
     * vowel, i.e. the ending begins with a vowel that coalesces with the stem's
     * final vowel.
     */
    static boolean isVowelInitial(String endingIast) {
        return startsWithVowelIast(endingIast);
    }

    private static final String IAST_VOWELS = "aāiīuūṛṝḷḹeo";

    private static boolean startsWithVowelIast(String ending) {
        if (ending == null || ending.isEmpty()) {
            return false;
        }
        return IAST_VOWELS.indexOf(ending.charAt(0)) >= 0;
    }

    private static String dropFinalVowel(String stem) {
        if (stem == null || stem.isEmpty()) {
            return stem;
        }
        char last = stem.charAt(stem.length() - 1);
        return IAST_VOWELS.indexOf(last) >= 0 ? stem.substring(0, stem.length() - 1) : stem;
    }

    private static final String DEVANAGARI_INDEPENDENT_VOWELS = "अआइईउऊऋॠऌॡएऐओऔ";
    private static final String DEVANAGARI_MATRAS = "ािीुूृॄेैोौ";
    private static final Set<Character> DEVANAGARI_CONSONANTS = Set.of(
            'क', 'ख', 'ग', 'घ', 'ङ', 'च', 'छ', 'ज', 'झ', 'ञ',
            'ट', 'ठ', 'ड', 'ढ', 'ण', 'त', 'थ', 'द', 'ध', 'न',
            'प', 'फ', 'ब', 'भ', 'म', 'य', 'र', 'ल', 'व', 'श', 'ष', 'स', 'ह');

    private static String dropFinalVowelDevanagari(String stem) {
        if (stem == null || stem.isEmpty()) {
            return stem;
        }
        char last = stem.charAt(stem.length() - 1);
        if (DEVANAGARI_MATRAS.indexOf(last) >= 0) {
            return stem.substring(0, stem.length() - 1);
        }
        if (DEVANAGARI_INDEPENDENT_VOWELS.indexOf(last) >= 0) {
            return stem.substring(0, stem.length() - 1);
        }
        if (DEVANAGARI_CONSONANTS.contains(last)) {
            return stem + '्';
        }
        return stem;
    }

    /**
     * Canonicalizes a coalesced vowel: a consonant + virāma + independent vowel
     * becomes consonant + the vowel's mātrā (or just the consonant for the
     * inherent {@code a}), matching the canonical spelling of full word forms.
     * E.g. {@code नर्+एन -> नरेन}.
     */
    private static String normalizeDevanagari(String word) {
        StringBuilder sb = new StringBuilder(word);
        StringBuilder normalized = new StringBuilder(word.length());
        for (int i = 0; i < sb.length(); i++) {
            char c = sb.charAt(i);
            if (c == '्' && i + 1 < sb.length() && isIndependentVowel(sb.charAt(i + 1))) {
                char v = sb.charAt(i + 1);
                String matra = matraOf(v);
                if (matra.isEmpty()) {
                    // inherent 'a' — drop the virrāma and the vowel
                } else {
                    normalized.append(matra);
                }
                i++; // consume the vowel
            } else {
                normalized.append(c);
            }
        }
        return normalized.toString();
    }

    private static boolean isIndependentVowel(char c) {
        return DEVANAGARI_INDEPENDENT_VOWELS.indexOf(c) >= 0;
    }

    private static String matraOf(char independentVowel) {
        return switch (independentVowel) {
            case 'अ' -> "";
            case 'आ' -> "ा";
            case 'इ' -> "ि";
            case 'ई' -> "ी";
            case 'उ' -> "ु";
            case 'ऊ' -> "ू";
            case 'ऋ' -> "ृ";
            case 'ॠ' -> "ॄ";
            case 'ऌ' -> "ॢ";
            case 'ए' -> "े";
            case 'ऐ' -> "ै";
            case 'ओ' -> "ो";
            case 'औ' -> "ौ";
            default -> String.valueOf(independentVowel);
        };
    }
}
