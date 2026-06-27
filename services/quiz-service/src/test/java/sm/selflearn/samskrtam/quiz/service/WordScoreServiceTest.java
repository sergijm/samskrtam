package sm.selflearn.samskrtam.quiz.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link WordScoreService#calculateScore(int, boolean)}.
 * Tests the exponential rating algorithm in isolation (no Spring context).
 * The full reactive pipeline (upsertScore) is tested by Agent 4 (Testing).
 */
class WordScoreServiceTest {

    // ==================== Algorithm boundary tests ====================

    @Test
    @DisplayName("First correct answer: score = 25")
    void firstCorrect_shouldBe25() {
        assertEquals(25, WordScoreService.calculateScore(0, true));
    }

    @Test
    @DisplayName("First incorrect answer: score = 5")
    void firstIncorrect_shouldBe5() {
        assertEquals(5, WordScoreService.calculateScore(0, false));
    }

    @Test
    @DisplayName("Convergence to 100 after repeated correct answers")
    void repeatedCorrect_convergesTo100() {
        int score = 0;
        // Simulate 5 correct answers in a row
        score = WordScoreService.calculateScore(score, true);  // 25
        assertEquals(25, score);
        score = WordScoreService.calculateScore(score, true);  // round5(62.5) = 65
        assertEquals(65, score);
        score = WordScoreService.calculateScore(score, true);  // round5(82.5) = 85
        assertEquals(85, score);
        score = WordScoreService.calculateScore(score, true);  // round5(92.5) = 95 (since 92.5/5=18.5 → round → 19*5=95)
        assertEquals(95, score);
        score = WordScoreService.calculateScore(score, true);  // round5(97.5) = 100
        assertEquals(100, score);
        // Next one stays at 100
        score = WordScoreService.calculateScore(score, true);  // 100 + (100-100)*0.5 = 100, round5=100
        assertEquals(100, score);
    }

    @Test
    @DisplayName("Degradation after incorrect answers: score decreases but never below 5")
    void repeatedIncorrect_degradation() {
        int score = 85; // Starting from high score
        score = WordScoreService.calculateScore(score, false); // 85 * 0.3 = 25.5 → round5 = 25
        assertEquals(25, score);
        score = WordScoreService.calculateScore(score, false); // 25 * 0.3 = 7.5 → round5 = 10 (7.5/5=1.5 → round → 2*5=10)
        assertEquals(10, score);
        score = WordScoreService.calculateScore(score, false); // 10 * 0.3 = 3.0 → round5 = 5, max(5,5) = 5
        assertEquals(5, score);
        // Keep going — should stay at 5
        score = WordScoreService.calculateScore(score, false); // 5 * 0.3 = 1.5 → round5 = 0 → max(0,5) = 5
        assertEquals(5, score);
        score = WordScoreService.calculateScore(score, false);
        assertEquals(5, score);
    }

    @Test
    @DisplayName("Score never goes below 5 after first answer")
    void scoreNeverBelow5() {
        // Even with extremely low starting values
        assertEquals(5, WordScoreService.calculateScore(5, false));  // 1.5 → 0 → max 5
        assertEquals(5, WordScoreService.calculateScore(7, false));  // 2.1 → 0 → max 5
        assertEquals(5, WordScoreService.calculateScore(10, false)); // 3.0 → 5
    }

    // ==================== round5 edge cases ====================

    @ParameterizedTest
    @CsvSource({
        "0, true, 25",
        "25, true, 65",    // 25 + 75*0.5 = 62.5 → round5 = 65
        "65, true, 85",    // 65 + 35*0.5 = 82.5 → round5 = 85
        "85, true, 95",    // 85 + 15*0.5 = 92.5 → round5 = 95
        "95, true, 100",   // 95 + 5*0.5 = 97.5 → round5 = 100
        "100, true, 100",  // already at 100
        "100, false, 30",  // 100*0.3 = 30 → round5 = 30
        "30, false, 10",   // 30*0.3 = 9 → round5 = 10
        "10, false, 5",    // 10*0.3 = 3 → round5 = 5
        "0, false, 5"      // first incorrect → 5
    })
    @DisplayName("Parameterized algorithm verification")
    void parameterizedScoreCalculation(int currentScore, boolean isCorrect, int expected) {
        assertEquals(expected, WordScoreService.calculateScore(currentScore, isCorrect));
    }
}