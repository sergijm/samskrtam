package sm.selflearn.samskrtam.curriculum.model;

/**
 * Coarse top-level topic classifier. Every {@link Topic} belongs to exactly one
 * domainType: either {@code GRAMMAR} (all fine-grained grammar domains) or
 * {@code LEXICON}.
 */
public enum TopicDomainType {
    GRAMMAR,
    LEXICON
}