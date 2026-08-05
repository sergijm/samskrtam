package sm.selflearn.samskrtam.curriculum.lexicon.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sm.selflearn.samskrtam.curriculum.lexicon.model.Lexeme;
import sm.selflearn.samskrtam.curriculum.lexicon.model.LexemeGender;
import sm.selflearn.samskrtam.curriculum.lexicon.model.LexemeStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LexemeRepository extends JpaRepository<Lexeme, UUID> {
    Optional<Lexeme> findByLemmaSlp1(String lemmaSlp1);

    Optional<Lexeme> findByLemmaSlp1AndGender(String lemmaSlp1, LexemeGender gender);

    boolean existsByLemmaSlp1AndGender(String lemmaSlp1, LexemeGender gender);

    List<Lexeme> findByStatus(LexemeStatus status);

    List<Lexeme> findByLemmaIastStartingWith(String prefix);
}