package sm.selflearn.samskrtam.sangraha.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sm.selflearn.samskrtam.sangraha.model.VerseWord;

import java.util.List;
import java.util.UUID;

@Repository
public interface VerseWordRepository extends JpaRepository<VerseWord, UUID> {

    List<VerseWord> findAllByVerseIdOrderByPositionAsc(UUID verseId);

    void deleteAllByVerseId(UUID verseId);
}