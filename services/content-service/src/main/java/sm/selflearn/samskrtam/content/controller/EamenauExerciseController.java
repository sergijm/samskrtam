package sm.selflearn.samskrtam.content.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sm.selflearn.samskrtam.content.dto.EamenauExerciseDetailDto;
import sm.selflearn.samskrtam.content.dto.EamenauExerciseDto;
import sm.selflearn.samskrtam.content.dto.SandhiRuleInfo;
import sm.selflearn.samskrtam.content.dto.SolutionDto;
import sm.selflearn.samskrtam.content.service.EamenauExerciseService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/eamenau/exercises")
@RequiredArgsConstructor
public class EamenauExerciseController {

    private final EamenauExerciseService eamenauExerciseService;

    @GetMapping
    public List<EamenauExerciseDto> getAllExercises() {
        return eamenauExerciseService.getAllExercises();
    }

    @GetMapping("/{id}")
    public EamenauExerciseDetailDto getExerciseById(@PathVariable Integer id) {
        return eamenauExerciseService.getExerciseById(id);
    }

    @GetMapping("/tasks/{taskId}/solution")
    public List<SolutionDto> getSolutionsForTask(@PathVariable Integer taskId) {
        return eamenauExerciseService.getSolutionsForTask(taskId);
    }

    @GetMapping("/{exerciseId}/sandhi-rules")
    public List<SandhiRuleInfo> getUniqueSandhiRulesForExercise(@PathVariable Integer exerciseId) {
        return eamenauExerciseService.getUniqueSandhiRulesForExercise(exerciseId);
    }
}
