package sm.selflearn.samskrtam.emenau.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sm.selflearn.samskrtam.content.dto.EmenauExerciseDetailDto;
import sm.selflearn.samskrtam.content.dto.EmenauExerciseDto;
import sm.selflearn.samskrtam.content.dto.SandhiRuleInfo;
import sm.selflearn.samskrtam.content.dto.SolutionDto;
import sm.selflearn.samskrtam.content.dto.SolutionUpdateRequestDto;
import sm.selflearn.samskrtam.content.service.EamenauExerciseService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/eamenau/exercises")
@RequiredArgsConstructor
@Slf4j
public class EmenauExerciseController {

    private final EamenauExerciseService eamenauExerciseService;

    @GetMapping
    public List<EmenauExerciseDto> getAllExercises() {
        return eamenauExerciseService.getAllExercises();
    }

    @GetMapping("/{id}")
    public EmenauExerciseDetailDto getExerciseById(@PathVariable Integer id) {
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

    @PutMapping("/solutions/{solutionId}")
    public ResponseEntity<Void> updateSolution(
            @PathVariable Integer solutionId,
            @RequestBody SolutionUpdateRequestDto requestDto) {
        log.info("Received update request for solutionId: {}. Data: {}", solutionId, requestDto);
        eamenauExerciseService.updateSolution(solutionId, requestDto);

        return ResponseEntity.ok().build();
    }
}

