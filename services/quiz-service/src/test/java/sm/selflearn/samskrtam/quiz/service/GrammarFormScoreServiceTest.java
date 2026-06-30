package sm.selflearn.samskrtam.quiz.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class GrammarFormScoreServiceTest {

    @ParameterizedTest
    @CsvSource({
            "0, true, 10",
            "90, true, 100",
            "30, false, 0",
            "60, false, 10",
            "100, false, 50"
    })
    @DisplayName("calculateScore returns correct new score")
    void calculateScore_returnsCorrectNewScore(int currentScore, boolean isCorrect, int expectedScore) {
        int actual = GrammarFormScoreService.calculateScore(currentScore, isCorrect);
        assertThat(actual).isEqualTo(expectedScore);
    }
}
