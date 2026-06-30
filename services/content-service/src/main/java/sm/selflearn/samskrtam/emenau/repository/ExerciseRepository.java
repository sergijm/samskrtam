package sm.selflearn.samskrtam.emenau.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sm.selflearn.samskrtam.emenau.model.Exercise;

import java.util.List;

@Repository
public interface ExerciseRepository extends JpaRepository<Exercise, Integer> {
    List<Exercise> findAllByOrderByExerciseNumberAscExerciseLetterAsc();
}
