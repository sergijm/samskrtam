package sm.selflearn.samskrtam.quiz.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import sm.selflearn.samskrtam.quiz.config.QuizGeneratorConfig;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ScoreCalculator}.
 * Tests the unified score formula §2.5 in isolation (no Spring context).
 *
 * <p>Параметры формулы: basePenalty=0.75, minPenalty=0.15, maxStability=10,
 * stabilityMistakeDrop=2, consecutiveMistakesThreshold=2.
 */
class ScoreCalculatorTest {

    private final QuizGeneratorConfig.ScoreParams defaultParams = createDefaultParams();

    private static QuizGeneratorConfig.ScoreParams createDefaultParams() {
        QuizGeneratorConfig.ScoreParams p = new QuizGeneratorConfig.ScoreParams();
        p.setBasePenalty(0.75);
        p.setMinPenalty(0.15);
        p.setMaxStability(10);
        p.setStabilityMistakeDrop(2);
        p.setConsecutiveMistakesThreshold(2);
        return p;
    }

    // ==================== Correct answers ====================

    @Test
    @DisplayName("First correct answer: score = 50, stability = 2")
    void firstCorrect_shouldBe50() {
        ScoreCalculator.Result r = ScoreCalculator.calculate(0, 1, 0, true, defaultParams);
        assertEquals(50, r.score());
        assertEquals(2, r.stability());
        assertEquals(0, r.consecutiveMistakes());
        assertFalse(r.stabilityReset());
    }

    @Test
    @DisplayName("Correct answers converge to 100")
    void repeatedCorrect_convergesTo100() {
        // Simulate from 0: 50 → 75 → 90 → 95 → 100
        int score = 0;
        int stability = 1;
        int mistakes = 0;

        ScoreCalculator.Result r = ScoreCalculator.calculate(score, stability, mistakes, true, defaultParams);
        assertEquals(50, r.score());
        assertEquals(2, r.stability());
        score = r.score(); stability = r.stability(); mistakes = r.consecutiveMistakes();

        r = ScoreCalculator.calculate(score, stability, mistakes, true, defaultParams);
        assertEquals(75, r.score());
        assertEquals(3, r.stability());
        score = r.score(); stability = r.stability();

        r = ScoreCalculator.calculate(score, stability, mistakes, true, defaultParams);
        assertEquals(90, r.score());
        assertEquals(4, r.stability());
        score = r.score(); stability = r.stability();

        r = ScoreCalculator.calculate(score, stability, mistakes, true, defaultParams);
        assertEquals(95, r.score());
        assertEquals(5, r.stability());
        score = r.score(); stability = r.stability();

        r = ScoreCalculator.calculate(score, stability, mistakes, true, defaultParams);
        assertEquals(100, r.score());
        assertEquals(6, r.stability());

        // Stay at 100
        r = ScoreCalculator.calculate(100, 6, 0, true, defaultParams);
        assertEquals(100, r.score());
    }

    @Test
    @DisplayName("Stability capped at maxStability = 10")
    void stability_cappedAtMax() {
        int stability = 9;
        ScoreCalculator.Result r = ScoreCalculator.calculate(100, stability, 0, true, defaultParams);
        assertEquals(10, r.stability());

        // Next one stays at 10
        r = ScoreCalculator.calculate(100, 10, 0, true, defaultParams);
        assertEquals(10, r.stability());
    }

    // ==================== Incorrect answers ====================

    @Test
    @DisplayName("First incorrect answer: score = 5, stability = 1")
    void firstIncorrect_shouldBe5() {
        ScoreCalculator.Result r = ScoreCalculator.calculate(0, 1, 0, false, defaultParams);
        // score = 0 - (0-5)*0.75 = 3.75 → round5 = 5
        assertEquals(5, r.score());
        assertEquals(1, r.stability());  // max(1, 1-2) = 1
        assertEquals(1, r.consecutiveMistakes());
        assertFalse(r.stabilityReset());
    }

    @Test
    @DisplayName("Error at high score with high stability: penalty is reduced")
    void errorAtHighStability_penaltyReduced() {
        // stability=6, score=100
        // penalty = 0.75/6 = 0.125 → clamp(0.15) = 0.15
        // score = 100 - (100-5)*0.15 = 100 - 95*0.15 = 100 - 14.25 = 85.75 → round5 = 85
        ScoreCalculator.Result r = ScoreCalculator.calculate(100, 6, 0, false, defaultParams);
        assertEquals(85, r.score());
        assertEquals(4, r.stability());  // 6-2=4
        assertEquals(1, r.consecutiveMistakes());
    }

    @Test
    @DisplayName("Two consecutive errors: stability drops to 1")
    void consecutiveErrors_resetStability() {
        // First error from high score
        ScoreCalculator.Result r = ScoreCalculator.calculate(100, 6, 0, false, defaultParams);
        assertEquals(85, r.score());
        assertEquals(4, r.stability());
        assertEquals(1, r.consecutiveMistakes());
        assertFalse(r.stabilityReset());

        // Second consecutive error — consecutiveMistakes >= 2 → stability = 1
        r = ScoreCalculator.calculate(85, 4, 1, false, defaultParams);
        // penalty = 0.75/4 = 0.1875, score = 85 - (85-5)*0.1875 = 85-15=70 → round5=70
        assertEquals(70, r.score());
        assertEquals(1, r.stability());  // reset!
        assertEquals(2, r.consecutiveMistakes());
        assertTrue(r.stabilityReset());
    }

    @Test
    @DisplayName("Series of errors converges to 5")
    void repeatedIncorrect_convergesTo5() {
        // Start from 100, stability=1
        int score = 100;
        int stability = 1;
        int mistakes = 0;

        ScoreCalculator.Result r;
        for (int i = 0; i < 10; i++) {
            r = ScoreCalculator.calculate(score, stability, mistakes, false, defaultParams);
            score = r.score();
            stability = r.stability();
            mistakes = r.consecutiveMistakes();
        }

        // Should converge to 5
        assertEquals(5, score);
        assertEquals(1, stability);
    }

    // ==================== Rounding edge cases ====================

    @ParameterizedTest
    @CsvSource({
        "0, true, 50",
        "50, true, 75",
        "75, true, 90",
        "90, true, 95",
        "95, true, 100",
        "0, false, 5",
        "100, true, 100",
        "100, false, 30",    // 100 - (100-5)*0.75 = 100-71.25=28.75→30
        "30, false, 10",     // 30 - (30-5)*0.75 = 30-18.75=11.25→10
        "10, false, 5"       // 10 - (10-5)*0.75 = 10-3.75=6.25→5
    })
    @DisplayName("Parameterized algorithm verification")
    void parameterizedCalculation(int prevScore, boolean isCorrect, int expectedScore) {
        ScoreCalculator.Result r = ScoreCalculator.calculate(prevScore, 1, 0, isCorrect, defaultParams);
        assertEquals(expectedScore, r.score());
    }

    // ==================== roundToNearest5 edge cases ====================

    @ParameterizedTest
    @CsvSource({
        "0.0, 0",
        "2.4, 0",
        "2.5, 0",   // < 50, halfway → floor
        "2.6, 5",
        "7.4, 5",
        "7.5, 10",  // < 50, halfway → floor
        "7.6, 10",
        "47.4, 45",
        "47.5, 45", // < 50, halfway → floor
        "52.5, 55", // >= 50, halfway → ceil
        "97.5, 100",
        "100.0, 100"
    })
    @DisplayName("roundToNearest5 edge cases")
    void roundToNearest5(double value, int expected) {
        assertEquals(expected, ScoreCalculator.roundToNearest5(value));
    }

    // ==================== Bucket determination ====================

    @Test
    @DisplayName("No row in DB → NEW")
    void noRow_isNew() {
        QuizGeneratorConfig.BucketParams bp = new QuizGeneratorConfig.BucketParams();
        bp.setDifficultUpperThreshold(30);
        bp.setMasteredLowerThreshold(80);

        assertEquals(ScoreCalculator.Bucket.NEW,
                ScoreCalculator.determineBucket(false, 0, bp));
    }

    @Test
    @DisplayName("Score <= 30 → DIFFICULT")
    void lowScore_isDifficult() {
        QuizGeneratorConfig.BucketParams bp = new QuizGeneratorConfig.BucketParams();
        bp.setDifficultUpperThreshold(30);
        bp.setMasteredLowerThreshold(80);

        assertEquals(ScoreCalculator.Bucket.DIFFICULT,
                ScoreCalculator.determineBucket(true, 5, bp));
        assertEquals(ScoreCalculator.Bucket.DIFFICULT,
                ScoreCalculator.determineBucket(true, 30, bp));
    }

    @Test
    @DisplayName("Score between 31 and 79 → LEARNING")
    void midScore_isLearning() {
        QuizGeneratorConfig.BucketParams bp = new QuizGeneratorConfig.BucketParams();
        bp.setDifficultUpperThreshold(30);
        bp.setMasteredLowerThreshold(80);

        assertEquals(ScoreCalculator.Bucket.LEARNING,
                ScoreCalculator.determineBucket(true, 50, bp));
        assertEquals(ScoreCalculator.Bucket.LEARNING,
                ScoreCalculator.determineBucket(true, 79, bp));
    }

    @Test
    @DisplayName("Score >= 80 → MASTERED")
    void highScore_isMastered() {
        QuizGeneratorConfig.BucketParams bp = new QuizGeneratorConfig.BucketParams();
        bp.setDifficultUpperThreshold(30);
        bp.setMasteredLowerThreshold(80);

        assertEquals(ScoreCalculator.Bucket.MASTERED,
                ScoreCalculator.determineBucket(true, 80, bp));
        assertEquals(ScoreCalculator.Bucket.MASTERED,
                ScoreCalculator.determineBucket(true, 100, bp));
    }

    // ==================== Hysteresis ====================

    @Test
    @DisplayName("Hysteresis: isAboveDifficultWithMargin")
    void hysteresis_difficultExit() {
        QuizGeneratorConfig.BucketParams bp = new QuizGeneratorConfig.BucketParams();
        bp.setDifficultUpperThreshold(30);
        bp.setDifficultExitMargin(5);

        // score <= 35 → still considered DIFFICULT with margin
        assertFalse(ScoreCalculator.isAboveDifficultWithMargin(35, bp));
        // score > 35 → above DIFFICULT with margin
        assertTrue(ScoreCalculator.isAboveDifficultWithMargin(36, bp));
    }

    @Test
    @DisplayName("Hysteresis: isBelowMasteredWithMargin")
    void hysteresis_masteredExit() {
        QuizGeneratorConfig.BucketParams bp = new QuizGeneratorConfig.BucketParams();
        bp.setMasteredLowerThreshold(80);
        bp.setMasteredExitMargin(5);

        // score >= 75 → still considered MASTERED with margin
        assertFalse(ScoreCalculator.isBelowMasteredWithMargin(75, bp));
        // score < 75 → below MASTERED with margin
        assertTrue(ScoreCalculator.isBelowMasteredWithMargin(74, bp));
    }
}