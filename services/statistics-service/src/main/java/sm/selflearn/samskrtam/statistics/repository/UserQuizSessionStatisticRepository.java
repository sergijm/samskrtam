package sm.selflearn.samskrtam.statistics.repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sm.selflearn.samskrtam.statistics.model.UserQuizSessionStatistic;
import java.util.Optional;
import java.util.UUID;
@Repository
public interface UserQuizSessionStatisticRepository
extends JpaRepository<UserQuizSessionStatistic, UUID> {
Page<UserQuizSessionStatistic> findByUserId(UUID userId, Pageable pageable);
Optional<UserQuizSessionStatistic> findByUserIdAndQuizId(UUID userId, UUID quizId);
}