package sm.selflearn.samskrtam.quiz.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Количество вопросов каждого типа declension-семейства за сессию, запрашиваемых
 * у curriculum-service (API v2) при старте сессии. Prefix {@code quiz.declension-session}.
 * Значения — дефолты, переопределяемые через env (application.yml) из
 * QUIZ_DECLENSION_*_COUNT. См. curriculum-quest-items.md §5.
 */
@Component
@ConfigurationProperties(prefix = "quiz.declension-session")
public class DeclensionSessionProperties {

    private int singleChoiceCount = 4;
    private int freeTextCount = 1;
    private int caseRecognitionCount = 4;
    private int matchCount = 1;

    public int getSingleChoiceCount() {
        return singleChoiceCount;
    }

    public void setSingleChoiceCount(int singleChoiceCount) {
        this.singleChoiceCount = singleChoiceCount;
    }

    public int getFreeTextCount() {
        return freeTextCount;
    }

    public void setFreeTextCount(int freeTextCount) {
        this.freeTextCount = freeTextCount;
    }

    public int getCaseRecognitionCount() {
        return caseRecognitionCount;
    }

    public void setCaseRecognitionCount(int caseRecognitionCount) {
        this.caseRecognitionCount = caseRecognitionCount;
    }

    public int getMatchCount() {
        return matchCount;
    }

    public void setMatchCount(int matchCount) {
        this.matchCount = matchCount;
    }
}
