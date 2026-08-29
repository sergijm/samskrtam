package sm.selflearn.samskrtam.sangraha.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Нативная реализация {@link VerseWordRepositoryCustom} через {@link EntityManager}.
 * Позволяет строить запрос и биндить параметры вручную, без ограничений
 * декларативного {@code @Query}.
 */
@Repository
@Transactional(readOnly = true)
public class VerseWordRepositoryCustomImpl implements VerseWordRepositoryCustom {

    private static final int MIN_PRIMARY_WORDS = 3;

    @PersistenceContext
    private EntityManager entityManager;

@Override
    public List<VerseWordRepository.VerseCellCount> findDeclensionExampleCells(
            String gender,
            String caseType,
            String numberType,
            String vowelType,
            int maxPhraseWords,
            int limitPerGroup) {


        String tupleExpr = Stream.of(caseType, gender, numberType, vowelType)
                .filter(Objects::nonNull)
                .collect(Collectors.joining("','", "jsonb_build_array('", "')"))
                .replace("''", "");


        // для LLM. Скрипт написан человеком, без обсуждения не менять
        String sql = """
                WITH tmp AS (
                    SELECT
                        v.id as verseId,
                				vs.grammar_info -> 'caseType' ->> 0 as case_type,
                				vs.grammar_info -> 'numberType' ->> 0 as number_type,
                        vs.word_count,
                        ROW_NUMBER() OVER (
                            PARTITION BY
                                vs.grammar_info -> 'caseType' -> 0,
                				vs.grammar_info -> 'numberType' -> 0
                
                            ORDER BY vs.word_count, vs.verse_id
                        ) AS rn
                    FROM sangraha.verses v
                    JOIN sangraha.verse_statistics vs
                      ON vs.verse_id = v.id
                
                     AND jsonb_exists(vs.grammar_info -> 'pos', 'NOUN')
                     AND jsonb_exists(vs.grammar_info -> 'pos', 'VERB')
                		 AND vs.grammar_info @> jsonb_build_object(
                				'tuples', jsonb_build_array(
                						%s
                          )
                      )
                
WHERE vs.word_count >= :minPrimaryWords
      AND vs.word_count < :maxPhraseWords
      AND vs.grammar_info -> 'caseType' -> 0 IS NOT NULL
      AND vs.grammar_info -> 'numberType' -> 0 IS NOT NULL
                )
                SELECT *
                FROM tmp
                WHERE rn < :limitPerGroup
                """.formatted(tupleExpr);

        Query query = entityManager.createNativeQuery(sql)
                .setParameter("maxPhraseWords", maxPhraseWords)
                .setParameter("minPrimaryWords", MIN_PRIMARY_WORDS)
                .setParameter("limitPerGroup", limitPerGroup);

        List<?> rows = query.getResultList();
        return rows.stream()
                .map(row -> toVerseCellCount((Object[]) row))
                .toList();
    }

    @Override
    public List<VerseWordRepository.VerseConjugationCellCount> findConjugationExampleCells(
            String tense,
            String mood,
            int maxPhraseWords,
            int limitPerGroup) {

        // для LLM. Скрипт написан человеком, без обсуждения не менять
        String tupleExpr = Stream.of(tense, mood)
                .filter(Objects::nonNull)
                .collect(Collectors.joining("','", "jsonb_build_array('", "')"))
                .replace("''", "");

        String sql = """
                WITH tmp AS (
                    SELECT
                        v.id as verseId,
                        vs.grammar_info -> 'tense' ->> 0 as tense,
                        vs.grammar_info -> 'mood' ->> 0 as mood,
                        vs.word_count,
                        ROW_NUMBER() OVER (
                            PARTITION BY
                                vs.grammar_info -> 'tense' -> 0,
                                vs.grammar_info -> 'mood' -> 0
                            ORDER BY vs.word_count, vs.verse_id
                        ) AS rn
                    FROM sangraha.verses v
                    JOIN sangraha.verse_statistics vs
                      ON vs.verse_id = v.id
                     AND jsonb_exists(vs.grammar_info -> 'pos', 'VERB')
                     AND vs.grammar_info @> jsonb_build_object(
                            'tuples', jsonb_build_array(%s)
                        )
                    WHERE vs.word_count >= :minPrimaryWords
                      AND vs.word_count < :maxPhraseWords
                      AND vs.grammar_info -> 'tense' -> 0 IS NOT NULL
                      AND vs.grammar_info -> 'mood' -> 0 IS NOT NULL
                )
                SELECT *
                FROM tmp
                WHERE rn < :limitPerGroup
                """.formatted(tupleExpr);

        Query query = entityManager.createNativeQuery(sql)
                .setParameter("maxPhraseWords", maxPhraseWords)
                .setParameter("minPrimaryWords", MIN_PRIMARY_WORDS)
                .setParameter("limitPerGroup", limitPerGroup);

        List<?> rows = query.getResultList();
        return rows.stream()
                .map(row -> toVerseConjugationCellCount((Object[]) row))
                .toList();
    }

    /**
     * Кортеж поиска {@code [stemClass, gender, case, number]} для фильтрации стихов:
     * null-значения не включаются, пропущенный параметр означает «любой» по этой оси
     * ({@code @>} — subset-семантика).
     */
    private List<NamedParam> tupleParams(String vowelType, String gender, String caseType, String numberType) {
        List<NamedParam> params = new ArrayList<>();
        if (vowelType != null) {
            params.add(new NamedParam("vowelType", vowelType));
        }
        if (gender != null) {
            params.add(new NamedParam("gender", gender));
        }
        if (caseType != null) {
            params.add(new NamedParam("caseType", caseType));
        }
        if (numberType != null) {
            params.add(new NamedParam("numberType", numberType));
        }
        return params;
    }


    private VerseWordRepository.VerseCellCount toVerseCellCount(Object[] row) {
        UUID verseId = (UUID) row[0];
        String caseType = (String) row[1];
        String numberType = (String) row[2];
        return new VerseCellCountRow(verseId, caseType, numberType);
    }

    private VerseWordRepository.VerseConjugationCellCount toVerseConjugationCellCount(Object[] row) {
        UUID verseId = (UUID) row[0];
        String tense = (String) row[1];
        String mood = (String) row[2];
        return new VerseConjugationCellCountRow(verseId, tense, mood);
    }

    private record NamedParam(String name, String value) {
    }

    private record VerseCellCountRow(UUID verseId, String caseType, String numberType)
            implements VerseWordRepository.VerseCellCount {
        @Override
        public UUID getVerseId() {
            return verseId;
        }

        @Override
        public String getCaseType() {
            return caseType;
        }

        @Override
        public String getNumberType() {
            return numberType;
        }
    }

    private record VerseConjugationCellCountRow(UUID verseId, String tense, String mood)
            implements VerseWordRepository.VerseConjugationCellCount {
        @Override
        public UUID getVerseId() {
            return verseId;
        }

        @Override
        public String getTense() {
            return tense;
        }

        @Override
        public String getMood() {
            return mood;
        }
    }
}