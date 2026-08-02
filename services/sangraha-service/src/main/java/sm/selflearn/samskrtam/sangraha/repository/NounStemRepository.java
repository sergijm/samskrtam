package sm.selflearn.samskrtam.sangraha.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sm.selflearn.samskrtam.sangraha.model.NounStem;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface NounStemRepository extends JpaRepository<NounStem, UUID> {

    List<NounStem> findAllByVerseWord_IdIn(Collection<UUID> verseWordIds);
}
