package sm.selflearn.samskrtam.curriculum.questgen;

import sm.selflearn.samskrtam.curriculum.model.Topic;

import java.util.Set;

/**
 * Base class of batch quiz-item generators. Each implementation owns a set of
 * topics (keyed by {@link Topic#getCode()}, the topic slug) and materializes
 * every {@code quest_item} row it can produce for a given topic. The set of
 * supported slugs drives the {@code Map<slug, QuizItemGenerator>} registry in
 * {@link QuizItemGenerationService}.
 */
public abstract class QuizItemGenerator {

    /**
     * The topic slugs this generator can produce quest items for. The registry
     * ({@link QuizItemGenerationService}) is keyed by exactly these codes; a
     * topic whose code is not listed is skipped during regeneration.
     */
    public abstract Set<String> supportedTopicSlugs();

    /**
     * Generates and persists quest items for a single topic.
     *
     * @param topic the topic (its {@code code} must be one of
     *              {@link #supportedTopicSlugs()})
     * @return number of quest items actually created for this topic
     */
    public abstract int generate(Topic topic);
}