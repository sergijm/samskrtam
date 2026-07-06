package sm.selflearn.samskrtam.quiz.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import sm.selflearn.samskrtam.quiz.model.DeclensionStem;

import java.util.UUID;

@Repository
public interface DeclensionStemRepository extends ReactiveCrudRepository<DeclensionStem, UUID> {

    Mono<DeclensionStem> findById(UUID id);
}