package sm.selflearn.samskrtam.curriculum.lexicon.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sm.selflearn.samskrtam.curriculum.lexicon.model.UserLexemeProgress;
import sm.selflearn.samskrtam.curriculum.lexicon.model.UserLexemeProgressId;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserLexemeProgressRepository extends JpaRepository<UserLexemeProgress, UserLexemeProgressId> {
    List<UserLexemeProgress> findByIdUserId(UUID userId);

    List<UserLexemeProgress> findByIdUserIdOrderByIdLexemeIdAsc(UUID userId);
}