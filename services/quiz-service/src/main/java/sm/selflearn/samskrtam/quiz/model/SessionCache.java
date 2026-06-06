package sm.selflearn.samskrtam.quiz.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import sm.selflearn.samskrtam.content.dto.QuizType; // Corrected import
import sm.selflearn.samskrtam.content.dto.VocabularyWordDto; // Corrected import

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Data
public class SessionCache {
    private UUID sessionId;
    private UUID userId;
    private UUID quizId;
    private QuizType quizType;
    private List<CachedQuestion> questions;
    private Set<UUID> answeredQuestionIds;
    private int score;
    private List<VocabularyWordDto> allVocabularyWords; // New field for vocabulary quizzes

    @JsonCreator
    @Builder // Keep @Builder for convenience in creating instances
    public SessionCache(
            @JsonProperty("sessionId") UUID sessionId,
            @JsonProperty("userId") UUID userId,
            @JsonProperty("quizId") UUID quizId,
            @JsonProperty("quizType") QuizType quizType,
            @JsonProperty("questions") List<CachedQuestion> questions,
            @JsonProperty("answeredQuestionIds") Set<UUID> answeredQuestionIds,
            @JsonProperty("score") int score,
            @JsonProperty("allVocabularyWords") List<VocabularyWordDto> allVocabularyWords) { // Updated constructor
        this.sessionId = sessionId;
        this.userId = userId;
        this.quizId = quizId;
        this.quizType = quizType;
        this.questions = questions;
        this.answeredQuestionIds = answeredQuestionIds;
        this.score = score;
        this.allVocabularyWords = allVocabularyWords;
    }

    public CachedQuestion findQuestion(UUID questionId) {
        return questions.stream()
                .filter(q -> q.getQuestionId().equals(questionId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Question not found in cache: " + questionId));
    }

    public void markAnswered(UUID questionId, boolean correct) {
        answeredQuestionIds.add(questionId);
        if (correct) {
            score++;
        }
    }
}
