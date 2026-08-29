package sm.selflearn.samskrtam.sangraha.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import sm.selflearn.samskrtam.content.model.VowelType;
import sm.selflearn.samskrtam.morphology.Gender;
import sm.selflearn.samskrtam.sangraha.model.NominalLemma;

import java.util.Collection;
import java.util.List;

@Repository
public interface NominalLemmaRepository extends JpaRepository<NominalLemma, Long> {

    List<NominalLemma> findByLemmaIastIn(Collection<String> lemmaIasts);

    /**
     * Кандидаты на импорт существительных по классу основы, отсортированные по
     * частоте в {@code verse_words} (по убыванию), затем по лемме. Частота
     * считается по всем вхождениям леммы — тот же источник ранжирования, что в
     * скрипте classify_nominal_lemmas.py. Возвращаются только строки с непустым
     * stem. {@code pageable} ограничивает выборку (limit).
     */
    @Query("""
            SELECT nl.lemmaIast AS lemmaIast, nl.stemIast AS stemIast,
                   nl.stemClass AS stemClass, COUNT(vw.id) AS frequency
            FROM NominalLemma nl
            LEFT JOIN VerseWord vw ON vw.lemmaIast = nl.lemmaIast
            WHERE nl.stemClass = :stemClass
              AND nl.stemIast IS NOT NULL
              AND nl.stemIast <> ''
            GROUP BY nl.lemmaIast, nl.stemIast, nl.stemClass
            ORDER BY COUNT(vw.id) DESC, nl.lemmaIast
            """)
    List<CandidateRow> findCandidatesByStemClass(@Param("stemClass") VowelType stemClass, Pageable pageable);

    /**
     * То же, что {@link #findCandidatesByStemClass}, но для всех классов.
     */
    @Query("""
            SELECT nl.lemmaIast AS lemmaIast, nl.stemIast AS stemIast,
                   nl.stemClass AS stemClass, COUNT(vw.id) AS frequency
            FROM NominalLemma nl
            LEFT JOIN VerseWord vw ON vw.lemmaIast = nl.lemmaIast
            WHERE nl.stemIast IS NOT NULL
              AND nl.stemIast <> ''
            GROUP BY nl.lemmaIast, nl.stemIast, nl.stemClass
            ORDER BY COUNT(vw.id) DESC, nl.lemmaIast
            """)
    List<CandidateRow> findAllCandidates(Pageable pageable);

    /**
     * Род(а) для заданных лемм по {@code verse_word_morphology} с частотой
     * каждого рода (количество вхождений). Используется для заполнения поля
     * gender у кандидатов на импорт (самый частый род по лемме).
     */
    @Query("""
            SELECT vw.lemmaIast AS lemmaIast, m.gender AS gender, COUNT(vw.id) AS cnt
            FROM VerseWord vw
            JOIN vw.morphology m
            WHERE vw.lemmaIast IN :lemmas
              AND m.gender IS NOT NULL
            GROUP BY vw.lemmaIast, m.gender
            """)
    List<LemmaGenderCount> countGenderByLemma(@Param("lemmas") Collection<String> lemmas);

    /** Строка кандидата: класс + основа + частотность. */
    interface CandidateRow {
        String getLemmaIast();

        String getStemIast();

        String getStemClass();

        long getFrequency();
    }

    /** Частотность рода леммы (одна строка на (лемма, род)). */
    interface LemmaGenderCount {
        String getLemmaIast();

        Gender getGender();

        long getCnt();
    }
}