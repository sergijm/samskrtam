package sm.selflearn.samskrtam.curriculum.questgen;

import org.springframework.stereotype.Service;
import sm.selflearn.samskrtam.curriculum.model.Topic;
import sm.selflearn.samskrtam.curriculum.model.TopicDomain;

import java.util.Set;

/**
 * Future CONJUNCTION quiz generator — a placeholder registered in the
 * per-domain generator registry. Currently produces nothing.
 */
@Service
public class ConjunctionQuizItemGenerator extends QuizItemGenerator {


    @Override
    public boolean isDomainSupported(TopicDomain domain) {
        return domain == TopicDomain.CONJUNCTION;
    }

    @Override
    public int generate(Topic topic) {
        return 0;
    }
}