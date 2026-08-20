package sm.selflearn.samskrtam.quiz.service;

import org.springframework.stereotype.Component;
import sm.selflearn.samskrtam.quiz.constants.ProgressConstants;
import sm.selflearn.samskrtam.quiz.dto.*;
import sm.selflearn.samskrtam.quiz.localization.CaseNumberGenderLocalizer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Серверная агрегация прогресса грамматического урока (вкладка «Прогресс»).
 *
 * <p>Ранее агрегирование выполнялось на фронте (функции aggregateByCase,
 * aggregateByNumber, aggregateByCaseAndNumber, aggregateByCasePair в
 * utils/grammarAggregation.ts). Теперь бэкенд отдаёт готовые агрегаты:
 *
 * <ul>
 *   <li>по падежам — {@link GrammarCaseAggregation} (заголовки строк сетки и срезы);</li>
 *   <li>по числам — {@link GrammarNumberAggregation} (заголовки столбцов и срезы);</li>
 *   <li>по паре падеж×число — {@link GrammarGridCellAggregation} (ячейки сетки);</li>
 *   <li>по семантическим парам падежей — {@link GrammarPairAggregation} (срезы progress-tag set).</li>
 * </ul>
 *
 * <p>Формулы идентичны фронтовым: aggregatedProgress = round(avg(score)),
 * learned = count(score &gt;= MASTERED_LOWER_THRESHOLD),
 * status по среднему (NEW при 0, LEARNING при &lt; порога, иначе MASTERED).
 * Порядок обхода — фиксированный CASE_TYPES × NUMBER_TYPES и CASE_PAIRS (равен
 * порядку фронтового {@code CASE_TYPES}/{@code NUMBER_TYPES}/{@code CASE_PAIRS}
 * и {@link ProgressTagSetId}).
 */
@Component
public class GrammarProgressAggregationService {

    public static final List<String> CASE_TYPES = List.of(
            "NOMINATIVE", "ACCUSATIVE", "INSTRUMENTAL", "DATIVE",
            "ABLATIVE", "GENITIVE", "LOCATIVE", "VOCATIVE");

    public static final List<String> NUMBER_TYPES = List.of(
            "SINGULAR", "DUAL", "PLURAL");

    public static final List<CasePair> CASE_PAIRS = List.of(
            new CasePair("GEN_LOC", "GENITIVE", "LOCATIVE"),
            new CasePair("GEN_ABL", "GENITIVE", "ABLATIVE"),
            new CasePair("DAT_ACC", "DATIVE", "ACCUSATIVE"),
            new CasePair("INS_ABL", "INSTRUMENTAL", "ABLATIVE"),
            new CasePair("INS_LOC", "INSTRUMENTAL", "LOCATIVE"),
            new CasePair("ACC_LOC", "ACCUSATIVE", "LOCATIVE"),
            new CasePair("DAT_GEN", "DATIVE", "GENITIVE"),
            new CasePair("ABL_LOC", "ABLATIVE", "LOCATIVE"));

    public record CasePair(String setId, String caseTypeA, String caseTypeB) {}

    public record ItemAgg(String caseType, String numberType, int score) {}

    public record GrammarProgressAggregations(
            List<GrammarCaseAggregation> caseAggregations,
            List<GrammarNumberAggregation> numberAggregations,
            List<GrammarGridCellAggregation> grid,
            List<GrammarPairAggregation> pairAggregations) {}

    public GrammarProgressAggregations aggregate(List<? extends ItemAgg> items) {
        return new GrammarProgressAggregations(
                aggregateByCase(items),
                aggregateByNumber(items),
                aggregateByCaseAndNumber(items),
                aggregateByPair(items));
    }

    public static ItemAgg from(GrammarQuestionProgress q) {
        return new ItemAgg(q.getCaseType(), q.getNumberType(), q.getScore());
    }

    private List<GrammarCaseAggregation> aggregateByCase(List<? extends ItemAgg> items) {
        Map<String, List<ItemAgg>> grouped = new LinkedHashMap<>();
        for (ItemAgg q : items) {
            grouped.computeIfAbsent(q.caseType(), k -> new ArrayList<>()).add(q);
        }
        List<GrammarCaseAggregation> result = new ArrayList<>();
        for (String caseType : CASE_TYPES) {
            List<ItemAgg> bucket = grouped.get(caseType);
            if (bucket == null || bucket.isEmpty()) continue;
            int progress = avgProgress(bucket);
            int learned = countLearned(bucket);
            result.add(new GrammarCaseAggregation(
                    caseType,
                    CaseNumberGenderLocalizer.caseTypeRu(caseType),
                    CaseNumberGenderLocalizer.caseTypeEn(caseType),
                    progress, bucket.size(), learned, progressStatus(progress)));
        }
        return result;
    }

    private List<GrammarNumberAggregation> aggregateByNumber(List<? extends ItemAgg> items) {
        Map<String, List<ItemAgg>> grouped = new LinkedHashMap<>();
        for (ItemAgg q : items) {
            grouped.computeIfAbsent(q.numberType(), k -> new ArrayList<>()).add(q);
        }
        List<GrammarNumberAggregation> result = new ArrayList<>();
        for (String numberType : NUMBER_TYPES) {
            List<ItemAgg> bucket = grouped.get(numberType);
            if (bucket == null || bucket.isEmpty()) continue;
            int progress = avgProgress(bucket);
            int learned = countLearned(bucket);
            result.add(new GrammarNumberAggregation(
                    numberType,
                    CaseNumberGenderLocalizer.numberTypeRu(numberType),
                    CaseNumberGenderLocalizer.numberTypeEn(numberType),
                    progress, bucket.size(), learned, progressStatus(progress)));
        }
        return result;
    }

    private List<GrammarGridCellAggregation> aggregateByCaseAndNumber(List<? extends ItemAgg> items) {
        Map<String, List<ItemAgg>> grouped = new LinkedHashMap<>();
        for (ItemAgg q : items) {
            grouped.computeIfAbsent(q.caseType() + ":" + q.numberType(), k -> new ArrayList<>()).add(q);
        }
        List<GrammarGridCellAggregation> result = new ArrayList<>();
        for (String caseType : CASE_TYPES) {
            for (String numberType : NUMBER_TYPES) {
                List<ItemAgg> bucket = grouped.get(caseType + ":" + numberType);
                if (bucket == null || bucket.isEmpty()) continue;
                int progress = avgProgress(bucket);
                int learned = countLearned(bucket);
                result.add(new GrammarGridCellAggregation(
                        caseType, numberType, progress, bucket.size(), learned,
                        progressStatus(progress)));
            }
        }
        return result;
    }

    private List<GrammarPairAggregation> aggregateByPair(List<? extends ItemAgg> items) {
        List<GrammarPairAggregation> result = new ArrayList<>();
        for (CasePair pair : CASE_PAIRS) {
            List<ItemAgg> bucket = new ArrayList<>();
            for (ItemAgg q : items) {
                if (q.caseType().equals(pair.caseTypeA()) || q.caseType().equals(pair.caseTypeB())) {
                    bucket.add(q);
                }
            }
            if (bucket.isEmpty()) continue;
            int progress = avgProgress(bucket);
            int learned = countLearned(bucket);
            result.add(new GrammarPairAggregation(
                    pair.setId(),
                    pair.caseTypeA(), pair.caseTypeB(),
                    CaseNumberGenderLocalizer.caseTypeRu(pair.caseTypeA()),
                    CaseNumberGenderLocalizer.caseTypeRu(pair.caseTypeB()),
                    CaseNumberGenderLocalizer.caseTypeEn(pair.caseTypeA()),
                    CaseNumberGenderLocalizer.caseTypeEn(pair.caseTypeB()),
                    progress, bucket.size(), learned, progressStatus(progress)));
        }
        return result;
    }

    private static int avgProgress(List<ItemAgg> items) {
        if (items.isEmpty()) return 0;
        int sum = items.stream().mapToInt(ItemAgg::score).sum();
        return (int) Math.round((double) sum / items.size());
    }

    private static int countLearned(List<ItemAgg> items) {
        return (int) items.stream()
                .filter(q -> q.score() >= ProgressConstants.MASTERED_LOWER_THRESHOLD)
                .count();
    }

    private static WordStatus progressStatus(int avg) {
        if (avg <= 0) return WordStatus.NEW;
        if (avg < ProgressConstants.MASTERED_LOWER_THRESHOLD) return WordStatus.LEARNING;
return WordStatus.MASTERED;
    }
}