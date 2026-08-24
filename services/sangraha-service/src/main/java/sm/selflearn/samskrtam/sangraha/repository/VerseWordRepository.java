package sm.selflearn.samskrtam.sangraha.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import sm.selflearn.samskrtam.sangraha.model.VerseWord;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;

@Repository
public interface VerseWordRepository extends JpaRepository<VerseWord, UUID>, VerseWordRepositoryCustom {

    List<VerseWord> findAllByVerse_IdOrderByPositionAsc(UUID verseId);

    /** Страница verse_words с id > :id (курсор) — проход по корпусу без загрузки стихов. */
    List<VerseWord> findAllByIdGreaterThanOrderByIdAsc(UUID id, Pageable pageable);

    /**
     * Различные (непустые) {@code lemma_iast} корпуса, которых ещё НЕТ в словаре
     * {@code lemma} (по тексту lemma_iast) — вход словаря LemmaRefreshService для
     * добавления новых строк. Пустые строки исключены (length > 0), NULL отсекается.
     */
    @Query("""
            SELECT DISTINCT vw.lemmaIast FROM VerseWord vw
            WHERE length(vw.lemmaIast) > 0
              AND vw.verse.chapterId IS NOT NULL
              AND NOT EXISTS (
                  SELECT 1 FROM Lemma l WHERE l.lemmaIast = vw.lemmaIast
              )
            """)
    List<String> findDistinctLemmaIast();

    /** Первые {@code position} строк словаря леммы (по тексту lemma_iast) — примеры словоформ для классификации. */
    List<VerseWord> findTop2ByLemmaIastOrderByPositionAsc(String lemmaIast);

    /**
     * Стихи, содержащие словоформу с точным surfaceIast (для колонки «примеры из
     * санграхи» в таблице слов урока склонений). Возвращает для каждой формы
     * самый короткий стих (min word_count), содержащий глагол (pos = VERB) и
     * имеющий 3–7 слов. DISTINCT ON — PostgreSQL, каждая форма получает ровно
     * один стих (самый короткий, при равенстве — первый по verse_id).
     * <p>
     * Если для формы нет подходящего стиха, она отсутствует в результате.
     * @see VerseWordExamplesService
     */
    @Query(value = """
            SELECT DISTINCT ON (vw.surface_iast)
                vw.surface_iast AS surfaceIast,
                vw.verse_id AS verseId,
                vs.word_count AS wordCount
            FROM sangraha.verse_words vw
            JOIN sangraha.verses v ON v.id = vw.verse_id
            JOIN sangraha.verse_statistics vs ON vs.verse_id = v.id
            WHERE vw.surface_iast IN (:surfaceIasts)
              AND v.deleted_at IS NULL
              AND vs.word_count BETWEEN :minWords AND :maxWords
              AND vs.grammar_info @> '{\"pos\": [\"VERB\"]}'::jsonb
            ORDER BY vw.surface_iast, vs.word_count
            """, nativeQuery = true)
    List<SurfaceVerseRank> findShortestSurfaceVerseWithVerb(
            @Param("surfaceIasts") List<String> surfaceIasts,
            @Param("minWords") int minWords,
            @Param("maxWords") int maxWords);

    interface SurfaceVerseRank {
        String getSurfaceIast();
        UUID getVerseId();
        int getWordCount();
    }

    /**
     * Для каждой леммы — до {@code limitPerLemma} стихов (по возрастанию длины
     * стиха в словах, при равенстве — по verse_id), содержащих эту лемму.
     * Дубликаты фраз убираются на уровне SQL: одна лемма может встречаться в
     * одном стихе несколько раз (несколько verse_words с тем же verse_id) и один
     * и тот же текст стиха может повторяться в корпусе — оба случая дают
     * повторяющуюся фразу, поэтому ранжируем по {@code (lemma_iast, text_iast)}
     * (phrase_rn = 1 оставляет только первую фразу) и лишь затем берём топ-N
     * на лемму (rn). Ищем только по verse_words реальных произведений
     * (chapter_id NOT NULL), чтобы у стиха гарантированно был workSlug.
     */
    @Query(value = """
            SELECT lemma_iast AS lemmaIast, verse_id AS verseId FROM (
                SELECT lemma_iast,
                       verse_id,
                       row_number() OVER (
                           PARTITION BY lemma_iast
                           ORDER BY word_count, verse_id_order
                       ) AS rn
                FROM (
                    SELECT vw.lemma_iast AS lemma_iast,
                           vw.verse_id AS verse_id,
                           vs.word_count AS word_count,
                           v.id AS verse_id_order,
                           row_number() OVER (
                               PARTITION BY vw.lemma_iast, v.text_iast
                               ORDER BY vs.word_count, v.id
                           ) AS phrase_rn
                    FROM sangraha.verse_words vw
                    JOIN sangraha.verses v ON v.id = vw.verse_id
                    JOIN sangraha.verse_statistics vs ON vs.verse_id = v.id
                    WHERE vw.lemma_iast IN (:lemmas)
                      AND v.deleted_at IS NULL
                      AND v.chapter_id IS NOT NULL
                ) dedup
                WHERE dedup.phrase_rn = 1
            ) ranked
            WHERE ranked.rn <= :limitPerLemma
            """, nativeQuery = true)
    List<LemmaVerseRank> findLemmaVerseIds(
            @Param("lemmas") List<String> lemmas,
            @Param("limitPerLemma") int limitPerLemma);

    interface LemmaVerseRank {
        String getLemmaIast();
        UUID getVerseId();
    }

    void deleteAllByVerse_Id(UUID verseId);

    /**
     * Одна строка = одно совпадение кортежа {@code [stemClass, gender, caseType, numberType]}
     * в {@code verse_statistics.grammar_info.tuples}: verseId + ячейка падежа/числа, в которую
     * он попадает. Один стих может дать несколько строк (несколько подходящих ячеек).
     */
    interface VerseCellCount {
        UUID getVerseId();

        String getCaseType();

        String getNumberType();
    }

    /**
     * Одна строка = одно совпадение кортежа {@code [person, tense, mood, voice]}
     * в {@code verse_statistics.grammar_info.tuples}: verseId + ячейка времени/наклонения.
     */
    interface VerseConjugationCellCount {
        UUID getVerseId();

        String getTense();

        String getMood();
    }
}
