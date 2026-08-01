package sm.selflearn.samskrtam.sangraha.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import sm.selflearn.samskrtam.sangraha.model.Gender;
import sm.selflearn.samskrtam.sangraha.model.GrammaticalCase;
import sm.selflearn.samskrtam.sangraha.model.NumberType;
import sm.selflearn.samskrtam.sangraha.model.VerseStatus;
import sm.selflearn.samskrtam.sangraha.model.VerseWord;

import java.util.List;
import java.util.UUID;

@Repository
public interface VerseWordRepository extends JpaRepository<VerseWord, UUID> {

    List<VerseWord> findAllByVerseIdOrderByPositionAsc(UUID verseId);

    void deleteAllByVerseId(UUID verseId);

    /**
     * Примеры склонений по словоизменительному классу: словоформа из verse_word_morphology
     * (gender/caseType/numberType) + основа (stem), оканчивающаяся на суффикс регулярного
     * класса (sangraha-service.md §9). Только стихи со status=ANALYZED и не удалённые.
     */
    @Query("""
            SELECT vw FROM VerseWord vw
            JOIN vw.morphology m
            WHERE m.gender = :gender AND m.caseType = :caseType AND m.numberType = :numberType
              AND vw.stem LIKE :stemSuffix
              AND vw.verseId IN (SELECT v.id FROM Verse v
                                 WHERE v.status = :status AND v.deletedAt IS NULL)
            """)
    List<VerseWord> findByMorphologyAndStemSuffix(
            @Param("gender") Gender gender,
            @Param("caseType") GrammaticalCase caseType,
            @Param("numberType") NumberType numberType,
            @Param("stemSuffix") String stemSuffix,
            @Param("status") VerseStatus status);

    /**
     * Примеры склонений для местоимённых классов: словоформа из verse_word_morphology
     * (gender/caseType/numberType) + фиксированная лемма (PRON_* → lemmaIast,
     * sangraha-service.md §9). Только стихи со status=ANALYZED и не удалённые.
     */
    @Query("""
            SELECT vw FROM VerseWord vw
            JOIN vw.morphology m
            WHERE m.gender = :gender AND m.caseType = :caseType AND m.numberType = :numberType
              AND vw.lemmaIast = :lemmaIast
              AND vw.verseId IN (SELECT v.id FROM Verse v
                                 WHERE v.status = :status AND v.deletedAt IS NULL)
            """)
    List<VerseWord> findByMorphologyAndLemmaIast(
            @Param("gender") Gender gender,
            @Param("caseType") GrammaticalCase caseType,
            @Param("numberType") NumberType numberType,
            @Param("lemmaIast") String lemmaIast,
            @Param("status") VerseStatus status);
}
