package sm.selflearn.samskrtam.content.repository;

import com.fasterxml.jackson.core.JsonParser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sm.selflearn.samskrtam.content.model.*;

import java.util.List;

@Repository
public interface CaseEndingRepository extends JpaRepository<CaseEnding, Long> {

    List<CaseEnding> findByVowelType(VowelType vowelType);

    List<CaseEnding> findByVowelTypeAndGenderAndCaseTypeAndNumberType(
            VowelType vowelType,
            Gender gender,
            CaseType caseType,
            NumberType numberType
    );

    /**
     * Все окончания для vowel_type с заданным endingIast.
     * Нужно для проверки «достаточно ли омонимов» для ENDING_MATCH.
     */
    List<CaseEnding> findByVowelTypeAndEndingIast(VowelType vowelType, String endingIast);
}