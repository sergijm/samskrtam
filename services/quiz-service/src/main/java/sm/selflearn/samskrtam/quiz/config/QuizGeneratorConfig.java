package sm.selflearn.samskrtam.quiz.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Конфигурация генератора квизов, инвариантная к типу квиза.
 * Значения по умолчанию — из спецификации §3.
 *
 * <p>В текущей итерации захардкожена; в дальнейшем может стать пользовательской настройкой.
 *
 * @see <a href="docs/quizzes/quiz-generator-spec.md#section-3">Спецификация §3</a>
 */
@Component
@ConfigurationProperties(prefix = "quiz.generator")
public class QuizGeneratorConfig {

    /** Параметры размера сессии. */
    private SessionSizeParams sessionSize = new SessionSizeParams();

    /** Параметры приоритизации due. */
    private DueSortParams dueSort = new DueSortParams();

    /** Параметры формулы score (§2.5). */
    private ScoreParams score = new ScoreParams();

    /** Параметры обработки ошибок в сессии. */
    private RequeueParams requeue = new RequeueParams();

    /** Параметры резерва. */
    private ReserveParams reserve = new ReserveParams();

    /** Общие ограничения. */
    private GeneralParams general = new GeneralParams();

    /** Пороги бакетов (§2.4). */
    private BucketParams buckets = new BucketParams();

    // --- Getters and setters ---

    public SessionSizeParams getSessionSize() { return sessionSize; }
    public void setSessionSize(SessionSizeParams sessionSize) { this.sessionSize = sessionSize; }

    public DueSortParams getDueSort() { return dueSort; }
    public void setDueSort(DueSortParams dueSort) { this.dueSort = dueSort; }

    public ScoreParams getScore() { return score; }
    public void setScore(ScoreParams score) { this.score = score; }

    public RequeueParams getRequeue() { return requeue; }
    public void setRequeue(RequeueParams requeue) { this.requeue = requeue; }

    public ReserveParams getReserve() { return reserve; }
    public void setReserve(ReserveParams reserve) { this.reserve = reserve; }

    public GeneralParams getGeneral() { return general; }
    public void setGeneral(GeneralParams general) { this.general = general; }

    public BucketParams getBuckets() { return buckets; }
    public void setBuckets(BucketParams buckets) { this.buckets = buckets; }

    // ============================================================
    // Inner parameter classes (records would be better but Spring
    // Boot configuration properties require mutable beans)
    // ============================================================

    /** Размер сессии. */
    public static class SessionSizeParams {
        /** Общее количество вопросов в сессии. */
        private int sessionSize = 10;
        /** Предел новых единиц за одну сессию. */
        private int maxNewPerSession = 3;
        /** Максимальная доля сессии для due-элементов. */
        private double dueCapRatio = 0.7;

        public int getSessionSize() { return sessionSize; }
        public void setSessionSize(int sessionSize) { this.sessionSize = sessionSize; }
        public int getMaxNewPerSession() { return maxNewPerSession; }
        public void setMaxNewPerSession(int maxNewPerSession) { this.maxNewPerSession = maxNewPerSession; }
        public double getDueCapRatio() { return dueCapRatio; }
        public void setDueCapRatio(double dueCapRatio) { this.dueCapRatio = dueCapRatio; }
    }

    /** Приоритизация due. */
    public static class DueSortParams {
        /** Стратегия сортировки: OVERDUE_FIRST, LOWEST_SCORE_FIRST, WEIGHTED. */
        private String dueSortStrategy = "WEIGHTED";
        /** Вес просроченности. */
        private double overdueWeight = 3.0;
        /** Вес низкого score. */
        private double scoreWeight = 2.0;
        /** Вес недавних ошибок. */
        private double mistakeWeight = 5.0;

        public String getDueSortStrategy() { return dueSortStrategy; }
        public void setDueSortStrategy(String dueSortStrategy) { this.dueSortStrategy = dueSortStrategy; }
        public double getOverdueWeight() { return overdueWeight; }
        public void setOverdueWeight(double overdueWeight) { this.overdueWeight = overdueWeight; }
        public double getScoreWeight() { return scoreWeight; }
        public void setScoreWeight(double scoreWeight) { this.scoreWeight = scoreWeight; }
        public double getMistakeWeight() { return mistakeWeight; }
        public void setMistakeWeight(double mistakeWeight) { this.mistakeWeight = mistakeWeight; }
    }

    /** Параметры формулы score (§2.5). */

    public static class ScoreParams {
        /** Базовый коэффициент штрафа за ошибку. */
        private double basePenalty = 0.75;
        /** Нижняя граница штрафа при высокой stability. */
        private double minPenalty = 0.15;
        /** Потолок роста stability. */
        private int maxStability = 10;
        /** На сколько падает stability при одной ошибке. */
        private int stabilityMistakeDrop = 2;
        /** Порог подряд идущих ошибок для сброса stability. */
        private int consecutiveMistakesThreshold = 2;

        public double getBasePenalty() { return basePenalty; }
        public void setBasePenalty(double basePenalty) { this.basePenalty = basePenalty; }
        public double getMinPenalty() { return minPenalty; }
        public void setMinPenalty(double minPenalty) { this.minPenalty = minPenalty; }
        public int getMaxStability() { return maxStability; }
        public void setMaxStability(int maxStability) { this.maxStability = maxStability; }
        public int getStabilityMistakeDrop() { return stabilityMistakeDrop; }
        public void setStabilityMistakeDrop(int stabilityMistakeDrop) { this.stabilityMistakeDrop = stabilityMistakeDrop; }
        public int getConsecutiveMistakesThreshold() { return consecutiveMistakesThreshold; }
        public void setConsecutiveMistakesThreshold(int consecutiveMistakesThreshold) { this.consecutiveMistakesThreshold = consecutiveMistakesThreshold; }
    }

    /** Обработка ошибок в сессии (requeue). */
    public static class RequeueParams {
        /** Повторный показ единицы с ошибкой в рамках текущей сессии. */
        private boolean requeueMistakeInSameSession = true;
        /** Минимальный сдвиг перед повторным показом (в вопросах). */
        private int requeueDelayPositions = 3;

        public boolean isRequeueMistakeInSameSession() { return requeueMistakeInSameSession; }
        public void setRequeueMistakeInSameSession(boolean requeueMistakeInSameSession) { this.requeueMistakeInSameSession = requeueMistakeInSameSession; }
        public int getRequeueDelayPositions() { return requeueDelayPositions; }
        public void setRequeueDelayPositions(int requeueDelayPositions) { this.requeueDelayPositions = requeueDelayPositions; }
    }

    /** Резерв. */
    public static class ReserveParams {
        /** Стратегия заполнения резерва: RANDOM, BY_CATEGORY_BALANCE, OLDEST_UNSEEN. */
        private String reserveFillStrategy = "RANDOM";
        /** Разрешить добивать сессию резервом при недостатке due и new. */
        private boolean allowReserveWhenNoDue = true;

        public String getReserveFillStrategy() { return reserveFillStrategy; }
        public void setReserveFillStrategy(String reserveFillStrategy) { this.reserveFillStrategy = reserveFillStrategy; }
        public boolean isAllowReserveWhenNoDue() { return allowReserveWhenNoDue; }
        public void setAllowReserveWhenNoDue(boolean allowReserveWhenNoDue) { this.allowReserveWhenNoDue = allowReserveWhenNoDue; }
    }

    /** Общие ограничения. */
    public static class GeneralParams {
        /** Минимальный интервал между повторами одной единицы (в вопросах сессии). */
        private int minGapBetweenSameWordRepeats = 5;
        /** Перемешивание подкатегорий. */
        private boolean interleaveCategories = true;
        /** Интервал контрольного показа MASTERED-единиц. */
        private int masteredCooldown = 7; // дней

        public int getMinGapBetweenSameWordRepeats() { return minGapBetweenSameWordRepeats; }
        public void setMinGapBetweenSameWordRepeats(int minGapBetweenSameWordRepeats) { this.minGapBetweenSameWordRepeats = minGapBetweenSameWordRepeats; }
        public boolean isInterleaveCategories() { return interleaveCategories; }
        public void setInterleaveCategories(boolean interleaveCategories) { this.interleaveCategories = interleaveCategories; }
        public int getMasteredCooldown() { return masteredCooldown; }
        public void setMasteredCooldown(int masteredCooldown) { this.masteredCooldown = masteredCooldown; }
    }

    /** Пороги бакетов (§2.4). */
    public static class BucketParams {
        /** Верхняя граница DIFFICULT. */
        private int difficultUpperThreshold = 30;
        /** Нижняя граница MASTERED. */
        private int masteredLowerThreshold = 80;
        /** Гистерезис при выходе из DIFFICULT. */
        private int difficultExitMargin = 5;
        /** Гистерезис при выходе из MASTERED. */
        private int masteredExitMargin = 5;

        public int getDifficultUpperThreshold() { return difficultUpperThreshold; }
        public void setDifficultUpperThreshold(int difficultUpperThreshold) { this.difficultUpperThreshold = difficultUpperThreshold; }
        public int getMasteredLowerThreshold() { return masteredLowerThreshold; }
        public void setMasteredLowerThreshold(int masteredLowerThreshold) { this.masteredLowerThreshold = masteredLowerThreshold; }
        public int getDifficultExitMargin() { return difficultExitMargin; }
        public void setDifficultExitMargin(int difficultExitMargin) { this.difficultExitMargin = difficultExitMargin; }
        public int getMasteredExitMargin() { return masteredExitMargin; }
        public void setMasteredExitMargin(int masteredExitMargin) { this.masteredExitMargin = masteredExitMargin; }
    }
}