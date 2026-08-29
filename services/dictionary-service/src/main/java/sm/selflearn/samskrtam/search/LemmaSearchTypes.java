package sm.selflearn.samskrtam.search;

/**
 * Internal row/value types for the fuzzy lemma search. Package-private on
 * purpose — only LemmaSearchService / LemmaSearchRepository touch them.
 */
record LemmaRow(long id, String dictionaryCode, String k1Slp1, String k2Original,
                 String headwordDisplay, long externalEntryId, String searchKey) {
}

record LemmaTrgmHit(LemmaRow row, double score) {
}

record DcsHit(String lemma, String lemmaKey, int frequency) {
}

record DcsTrgmHit(String surfaceForm, String lemma, String lemmaKey, int frequency, double score) {
}

record EndingPair(String ending, String lemmaSuffix) {
}
