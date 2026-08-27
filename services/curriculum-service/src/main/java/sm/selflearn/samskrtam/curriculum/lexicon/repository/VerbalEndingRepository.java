package sm.selflearn.samskrtam.curriculum.lexicon.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sm.selflearn.samskrtam.curriculum.lexicon.lingua.VerbalEnding;

@Repository
public interface VerbalEndingRepository extends JpaRepository<VerbalEnding, Integer> {
}