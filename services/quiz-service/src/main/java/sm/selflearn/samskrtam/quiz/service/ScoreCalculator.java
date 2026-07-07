package sm.selflearn.samskrtam.quiz.service;

import sm.selflearn.samskrtam.quiz.config.QuizGeneratorConfig;

/**
 * Чистая функция расчёта score и stability по формуле §2.5 специификации quiz-generator.
 * Не имеет зависимостей от Spring/БД — модульно тестируемая отдельно от generate().
 *
 * <p>Инварианты: score ∈ [0, 100], без строки в quiz_item_score = NEW.
 * Точка равновесия ошибки = 5 (не 0).
 *
 * <p>Параметры формулы конфигурируются через {@link QuizGeneratorConfig.ScoreParams}.
 *
 * @see <a href="docs/quizzes/quiz-generator-spec.md#section-2.5">Спецификация §2.5</a>
 */
public final class ScoreCalculator {

    private ScoreCalculator() {
        // утилитный класс
    }

    /**
     * Результат расчёта для одного ответа.
     */
    public static final class Result {
        private final int score;
        private final int stability;
        private final int consecutiveMistakes;
        private final boolean stabilityReset;

        public Result(int score, int stability, int consecutiveMistakes, boolean stabilityReset) {
            this.score = score;
            this.stability = stability;
            this.consecutiveMistakes = consecutiveMistakes;
            this.stabilityReset = stabilityReset;
        }

        public int score() { return score; }
        public int stability() { return stability; }
        public int consecutiveMistakes() { return consecutiveMistakes; }
        public boolean stabilityReset() { return stabilityReset; }
    }

    /**
     * Расчёт нового score, stability и consecutiveMistakes.
     *
     * @param prevScore          предыдущий score (0, если нет строки в БД)
     * @param prevStability      предыдущая stability (1, если нет строки в БД)
     * @param prevConsecutiveMistakes предыдущее значение consecutiveMistakes
     * @param isCorrect          ответ правильный?
     * @param params             параметры формулы
     * @return новый (score, stability, consecutiveMistakes, stabilityReset)
     */
    public static Result calculate(
            int prevScore,
            int prevStability,
            int prevConsecutiveMistakes,
            boolean isCorrect,
            QuizGeneratorConfig.ScoreParams params) {

        int newConsecutiveMistakes;
        int newStability;
        double rawScore;
        boolean stabilityReset = false;

        if (isCorrect) {
            // Правильный ответ
            rawScore = prevScore + (100.0 - prevScore) * 0.5;
            newStability = Math.min(params.getMaxStability(), prevStability + 1);
            newConsecutiveMistakes = 0;
        } else {
            // Неправильный ответ
            double penalty = params.getBasePenalty() / prevStability;
            // clamp(minPenalty, basePenalty)
            if (penalty < params.getMinPenalty()) {
                penalty = params.getMinPenalty();
            }
            if (penalty > params.getBasePenalty()) {
                penalty = params.getBasePenalty();
            }
            rawScore = prevScore - (prevScore - 5.0) * penalty;

            newStability = Math.max(1, prevStability - params.getStabilityMistakeDrop());
            newConsecutiveMistakes = prevConsecutiveMistakes + 1;

            if (newConsecutiveMistakes >= params.getConsecutiveMistakesThreshold()) {
                newStability = 1;
                stabilityReset = true;
            }
        }

        // Округление до ближайшего кратного 5
        int roundedScore = roundToNearest5(rawScore);

        // clamp(0, 100)
        if (roundedScore < 0) roundedScore = 0;
        if (roundedScore > 100) roundedScore = 100;

        return new Result(roundedScore, newStability, newConsecutiveMistakes, stabilityReset);
    }

    /**
     * Округление до ближайшего кратного 5.
     * На равном расстоянии между двумя кратными 5:
     * — вниз, если сырое значение &lt; 50
     * — вверх, если ≥ 50
     */
    static int roundToNearest5(double value) {
        double quotient = value / 5.0;
        long rounded;
        if (Math.abs(quotient - Math.floor(quotient)) == 0.5) {
            // Точная середина
            if (value < 50.0) {
                rounded = (long) Math.floor(quotient);
            } else {
                rounded = (long) Math.ceil(quotient);
            }
        } else {
            rounded = Math.round(quotient);
        }
        return (int) (rounded * 5);
    }

    /**
     * Определение бакета (статуса) по score.
     * Статус не хранится — вычисляется лениво при чтении (§2.4 спеки).
     *
     * @param hasRow  есть ли строка в quiz_item_score (false = NEW)
     * @param score   текущий score (игнорируется, если hasRow == false)
     * @param params  параметры порогов
     * @return бакет
     */
    public static Bucket determineBucket(
            boolean hasRow,
            int score,
            QuizGeneratorConfig.BucketParams params) {

        if (!hasRow) {
            return Bucket.NEW;
        }

        if (score <= params.getDifficultUpperThreshold()) {
            return Bucket.DIFFICULT;
        }

        if (score >= params.getMasteredLowerThreshold()) {
            return Bucket.MASTERED;
        }

        return Bucket.LEARNING;
    }

    /**
     * Проверка, находится ли единица вне бакета DIFFICULT с учётом гистерезиса.
     * Используется, чтобы единица не "прыгала" между бакетами от колебаний score на 1-2 пункта.
     */
    public static boolean isAboveDifficultWithMargin(
            int score,
            QuizGeneratorConfig.BucketParams params) {
        return score > params.getDifficultUpperThreshold() + params.getDifficultExitMargin();
    }

    /**
     * Проверка, находится ли единица вне бакета MASTERED с учётом гистерезиса.
     */
    public static boolean isBelowMasteredWithMargin(
            int score,
            QuizGeneratorConfig.BucketParams params) {
        return score < params.getMasteredLowerThreshold() - params.getMasteredExitMargin();
    }

    /**
     * Бакеты (производный статус).
     */
    public enum Bucket {
        NEW,
        DIFFICULT,
        LEARNING,
        MASTERED
    }
}