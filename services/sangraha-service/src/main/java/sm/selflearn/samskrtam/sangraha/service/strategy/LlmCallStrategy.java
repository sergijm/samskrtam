package sm.selflearn.samskrtam.sangraha.service.strategy;

import sm.selflearn.samskrtam.sangraha.model.Verse;

import java.util.List;

/**
 * Strategy interface for LLM invocation (single-pass via tool_choice forced).
 */
public interface LlmCallStrategy {

    /**
     * Executes the LLM call for the given list of verses and returns the raw response
     * tree together with the raw prompt (request body) that was sent.
     */
    LlmCallResult call(List<Verse> verses) throws Exception;

    /**
     * Returns the name of this strategy for logging.
     */
    String getName();
}