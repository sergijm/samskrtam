package sm.selflearn.samskrtam.quiz.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import sm.selflearn.samskrtam.content.dto.LessonType;
import sm.selflearn.samskrtam.quiz.model.SessionHistory;

import java.util.UUID;

@Repository
public interface SessionHistoryRepository extends ReactiveCrudRepository<SessionHistory, UUID> {
    Flux<SessionHistory> findByUserId(UUID userId, Pageable pageable);
    Flux<SessionHistory> findByUserIdAndLessonId(UUID userId, UUID lessonId, Pageable pageable);
    Flux<SessionHistory> findByUserIdAndLessonType(UUID userId, LessonType lessonType, Pageable pageable);
    Flux<SessionHistory> findByUserIdAndLessonIdAndLessonType(UUID userId, UUID lessonId, LessonType lessonType, Pageable pageable);
}

