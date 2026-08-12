package sm.selflearn.samskrtam.curriculum.questgen;

import org.springframework.stereotype.Service;
import sm.selflearn.samskrtam.curriculum.model.Topic;

import java.util.Set;

/**
 * Future LEXICAL quiz generator — a placeholder registered in the
 * {@code Map<slug, QuizItemGenerator>} registry. Currently supports no topic
 * slugs and produces nothing.
 */
@Service
public class LexicalQuizItemGenerator extends QuizItemGenerator {

    @Override
    public Set<String> supportedTopicSlugs() {
        return Set.of();
    }

    @Override
    public int generate(Topic topic) {
        return 0;
    }
}