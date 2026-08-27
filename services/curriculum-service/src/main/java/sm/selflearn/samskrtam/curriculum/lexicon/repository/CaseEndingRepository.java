package sm.selflearn.samskrtam.curriculum.lexicon.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sm.selflearn.samskrtam.curriculum.lexicon.lingua.CaseEnding;
import sm.selflearn.samskrtam.curriculum.lexicon.lingua.StemTypeEnum;
import sm.selflearn.samskrtam.curriculum.lexicon.model.LexemeGender;
import sm.selflearn.samskrtam.curriculum.questgen.morphology.CaseType;
import sm.selflearn.samskrtam.morphology.NumberType;

import java.util.List;

@Repository
public interface CaseEndingRepository extends JpaRepository<CaseEnding, Integer> {
    List<CaseEnding> findByStemTypeAndGenderAndNumberAndGrammaticalCase(
            StemTypeEnum stemType,
            LexemeGender gender,
            NumberType number,
            CaseType grammaticalCase);
}
