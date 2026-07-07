package sm.selflearn.samskrtam.sangraha.service.strategy;

import sm.selflearn.samskrtam.sangraha.model.Verse;

/**
 * Strategy interface for LLM invocation modes (single-pass, two-pass).
 */
public interface LlmCallStrategy {

    /**
     * Executes the LLM call for the given verse and returns the raw response tree.
     */
    Object call(Verse verse) throws Exception;

    /**
     * Returns the name of this strategy for logging.
     */
    String getName();
}