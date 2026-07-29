package sm.selflearn.samskrtam.content.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import sm.selflearn.samskrtam.content.model.DeclensionStem;
import sm.selflearn.samskrtam.content.model.Gender;
import sm.selflearn.samskrtam.content.model.VowelType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeclensionStemRepository extends JpaRepository<DeclensionStem, UUID> {
    Optional<DeclensionStem> findByStemIast(String stemIast);
    List<DeclensionStem> findByVowelType(VowelType vowelType);

    /**
     * Find stems by multiple vowel types (for compound lessons like declensions-i-u).
     */
    List<DeclensionStem> findByVowelTypeIn(List<VowelType> vowelTypes);

    /**
     * Find stems by multiple vowel types AND multiple genders (for ALL_STEMS cross-lesson quiz).
     * Empty lists mean "no filter" on that axis.
     */
    @Query("SELECT ds FROM DeclensionStem ds WHERE " +
           "(:vowelTypes IS NULL OR ds.vowelType IN :vowelTypes) AND " +
           "(:genders IS NULL OR ds.gender IN :genders)")
    List<DeclensionStem> findByVowelTypeInAndGenderIn(
            @Param("vowelTypes") List<VowelType> vowelTypes,
            @Param("genders") List<Gender> genders);
}

