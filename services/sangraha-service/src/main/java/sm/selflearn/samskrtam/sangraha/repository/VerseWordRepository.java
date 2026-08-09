package sm.selflearn.samskrtam.sangraha.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import sm.selflearn.samskrtam.sangraha.model.Gender;
import sm.selflearn.samskrtam.sangraha.model.GrammaticalCase;
import sm.selflearn.samskrtam.sangraha.model.NumberType;
import sm.selflearn.samskrtam.sangraha.model.VerseWord;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;

@Repository
public interface VerseWordRepository extends JpaRepository<VerseWord, UUID> {

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

    void deleteAllByVerse_Id(UUID verseId);

    /**
     * Кандидаты на попадание в группу для регулярного класса: стихи, у которых есть
     * словоформа с verse_word_morphology (gender/caseType/numberType), класс основы
     * которой (vowelType) совпадает с :vowelTypeName. Класс основы определяется по
     * nominal_lemmas (stemClass строки на лемму слова, join по тексту lemmaIast — без
     * физической FK-связи); если для леммы нет строки, stemClass в ней null или не
     * является значением VowelType (:validStemClasses) — fallback по последней букве
     * stem (:lastLetters, sangraha-service.md §9). Рейтинг — по полной длине стиха в
     * словах (COUNT всех verse_words, не только совпавших словоформ), поэтому count
     * считается через LEFT JOIN verse_words, а фильтрация — через EXISTS. Поиск идёт
     * напрямую по наличию подходящей verse_word_morphology без условия на verses.status
     * (часть корпуса загружена внешним скриптом в обход штатного флоу анализа, у таких
     * стихов морфология уже создана, а status не обязательно ANALYZED).
     */
    @Query("""
            SELECT v.id AS verseId, COUNT(w.id) AS wordCount
            FROM Verse v
            LEFT JOIN v.verseWords w
            WHERE v.deletedAt IS NULL
              AND EXISTS (
                    SELECT 1
                    FROM VerseWord vw
                    JOIN vw.morphology m
                    LEFT JOIN NominalLemma nl ON nl.lemmaIast = vw.lemmaIast
                    WHERE vw.verse.id = v.id
                      AND m.gender = :gender
                      AND m.caseType = :caseType
                      AND m.numberType = :numberType
                      AND (
                            nl.stemClass = :vowelTypeName
                            OR (
                                (nl IS NULL OR nl.stemClass IS NULL OR nl.stemClass NOT IN :validStemClasses)
                                AND substring(vw.stem, length(vw.stem)) IN :lastLetters
                            )
                      )
              )
            GROUP BY v.id
            HAVING COUNT(w.id) <= :maxPhraseWords
            """)
    List<VerseWordCount> findVerseWordCountsByVowelType(
            @Param("gender") Gender gender,
            @Param("caseType") GrammaticalCase caseType,
            @Param("numberType") NumberType numberType,
            @Param("vowelTypeName") String vowelTypeName,
            @Param("validStemClasses") Collection<String> validStemClasses,
            @Param("lastLetters") Collection<String> lastLetters,
            @Param("maxPhraseWords") int maxPhraseWords);

    /**
     * Кандидаты на попадание в группу для местоимённого класса: стихи, у которых есть
     * словоформа с verse_word_morphology (gender/caseType/numberType) и фиксированной
     * леммой (PRON_* → lemmaIast, sangraha-service.md §9). Тот же принцип: рейтинг по
     * полной длине стиха, фильтрация через EXISTS, без условия на verses.status.
     */
    @Query("""
            SELECT v.id AS verseId, COUNT(w.id) AS wordCount
            FROM Verse v
            LEFT JOIN v.verseWords w
            WHERE v.deletedAt IS NULL
              AND EXISTS (
                    SELECT 1
                    FROM VerseWord vw
                    JOIN vw.morphology m
                    WHERE vw.verse.id = v.id
                      AND m.gender = :gender
                      AND m.caseType = :caseType
                      AND m.numberType = :numberType
                      AND vw.lemmaIast = :lemmaIast
              )
            GROUP BY v.id
            HAVING COUNT(w.id) <= :maxPhraseWords
            """)
    List<VerseWordCount> findVerseWordCountsByLemmaIast(
            @Param("gender") Gender gender,
            @Param("caseType") GrammaticalCase caseType,
            @Param("numberType") NumberType numberType,
            @Param("lemmaIast") String lemmaIast,
            @Param("maxPhraseWords") int maxPhraseWords);

    interface VerseWordCount {
        UUID getVerseId();

        long getWordCount();
    }
}
