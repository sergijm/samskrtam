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
}
