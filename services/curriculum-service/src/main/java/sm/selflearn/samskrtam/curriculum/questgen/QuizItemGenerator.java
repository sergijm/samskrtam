package sm.selflearn.samskrtam.curriculum.questgen;

import sm.selflearn.samskrtam.curriculum.model.Topic;
import sm.selflearn.samskrtam.curriculum.model.TopicDomain;

import java.util.Set;

/**
 * Base class of batch quiz-item generators.
 */
public abstract class QuizItemGenerator {

    public abstract boolean isDomainSupported(TopicDomain domain);

    public abstract int generate(Topic topic);

    public void ensureTopicsExist() {}
}