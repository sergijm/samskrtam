package sm.selflearn.samskrtam.sangraha.service.strategy;

/**
 * Этап LLM-анализа стиха (см. sangraha-service.md §5.1).
 * STEP1 — translation + external sandhi + лексико-морфология (tool submit_verse_analyses_step1).
 * STEP2 — внутренние сандхи / словообразование (tool submit_word_formations), запускается
 *         отдельно, по явному запросу, не автоматически после шага 1.
 */
public enum LlmStep {
    STEP1,
    STEP2
}
