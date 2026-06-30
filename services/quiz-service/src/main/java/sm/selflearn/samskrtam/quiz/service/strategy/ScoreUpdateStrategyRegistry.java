package sm.selflearn.samskrtam.quiz.service.strategy;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import sm.selflearn.samskrtam.content.dto.LessonType;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ScoreUpdateStrategyRegistry {

    private final List<ScoreUpdateStrategy> strategies;
    private final Map<LessonType, ScoreUpdateStrategy> strategyMap = new EnumMap<>(LessonType.class);

    @PostConstruct
    public void init() {
        for (LessonType type : LessonType.values()) {
            strategyMap.put(type, findStrategy(type));
        }
    }

    public ScoreUpdateStrategy getStrategy(LessonType lessonType) {
        return strategyMap.getOrDefault(lessonType, new NoopScoreUpdateStrategy());
    }

    private ScoreUpdateStrategy findStrategy(LessonType lessonType) {
        return strategies.stream()
                .filter(s -> s.supports(lessonType))
                .findFirst()
                .orElse(new NoopScoreUpdateStrategy());
    }
}