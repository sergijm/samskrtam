package sm.selflearn.samskrtam.quiz.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Количество вопросов каждого типа conjugation-семейства за сессию.
 * Prefix {@code quiz.conjugation-session}.
 */
@Component
@ConfigurationProperties(prefix = "quiz.conjugation-session")
public class ConjugationSessionProperties {

    private int singleChoiceCount = 4;
    private int freeTextCount = 1;
    private int matchCount = 1;
    private int analysisCount = 4;

    public int getSingleChoiceCount() { return singleChoiceCount; }
    public void setSingleChoiceCount(int v) { this.singleChoiceCount = v; }

    public int getFreeTextCount() { return freeTextCount; }
    public void setFreeTextCount(int v) { this.freeTextCount = v; }

    public int getMatchCount() { return matchCount; }
    public void setMatchCount(int v) { this.matchCount = v; }

    public int getAnalysisCount() { return analysisCount; }
    public void setAnalysisCount(int v) { this.analysisCount = v; }
}