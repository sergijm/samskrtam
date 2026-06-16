package sm.selflearn.samskrtam.content.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import sm.selflearn.samskrtam.content.dto.EamenauExerciseDetailDto;
import sm.selflearn.samskrtam.content.dto.EamenauExerciseDto;
import sm.selflearn.samskrtam.content.dto.EamenauTaskDto;
import sm.selflearn.samskrtam.content.dto.SandhiRuleInfo;
import sm.selflearn.samskrtam.content.dto.SolutionDto;
import sm.selflearn.samskrtam.eamenau.model.*;
import sm.selflearn.samskrtam.eamenau.repository.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.ArrayList; // Import ArrayList

@Service
@RequiredArgsConstructor
@Slf4j
public class EamenauExerciseService {

    private final ExerciseRepository exerciseRepository;
    private final TaskRepository taskRepository;
    private final SolutionRepository solutionRepository;
    private final SolutionSandhiRuleRepository solutionSandhiRuleRepository;
    private final SandhiRuleRepository sandhiRuleRepository;

    public List<EamenauExerciseDto> getAllExercises() {
        return exerciseRepository.findAllByOrderByExerciseNumberAscExerciseLetterAsc().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public EamenauExerciseDetailDto getExerciseById(Integer id) {
        return exerciseRepository.findById(id)
                .map(this::mapToDetailDto)
                .orElse(null);
    }

    public List<SolutionDto> getSolutionsForTask(Integer taskId) {
        return solutionRepository.findByTaskIdAndIsCorrect(taskId, true).stream()
                .map(this::mapToSolutionDto)
                .collect(Collectors.toList());
    }

    public List<SandhiRuleInfo> getUniqueSandhiRulesForExercise(Integer exerciseId) {
        List<Task> tasks = taskRepository.findByExerciseIdOrderByTaskNumberAsc(exerciseId);
        Set<Integer> uniqueSolutionIds = tasks.stream()
                .flatMap(task -> solutionRepository.findByTaskIdAndIsCorrect(task.getId(), true).stream())
                .map(Solution::getId)
                .collect(Collectors.toSet());

        Set<Integer> uniqueSandhiRuleNumbers = uniqueSolutionIds.stream()
                .flatMap(solutionId -> solutionSandhiRuleRepository.findBySolutionId(solutionId).stream())
                .map(SolutionSandhiRule::getSandhiRuleId)
                .collect(Collectors.toSet());

        // Convert Set to List for the repository method
        List<Integer> uniqueSandhiRuleNumbersList = new ArrayList<>(uniqueSandhiRuleNumbers);

        return sandhiRuleRepository.findByRuleNumberIn(uniqueSandhiRuleNumbersList).stream()
                .map(rule -> SandhiRuleInfo.builder()
                        .ruleNumber(rule.getRuleNumber())
                        .shortDescription(rule.getShortDescription())
                        .build())
                .distinct()
                .collect(Collectors.toList());
    }

    private SolutionDto mapToSolutionDto(Solution solution) {
        List<Integer> sandhiRuleNumbersFromSolution = solutionSandhiRuleRepository.findBySolutionId(solution.getId()).stream()
                .map(SolutionSandhiRule::getSandhiRuleId) // Assuming sandhiRuleId here is actually rule_number
                .collect(Collectors.toList());
        
        log.debug("Found sandhiRuleNumbersFromSolution: {}", sandhiRuleNumbersFromSolution);

        List<SandhiRuleInfo> sandhiRuleInfos = sandhiRuleRepository.findByRuleNumberIn(sandhiRuleNumbersFromSolution).stream()
                .map(rule -> SandhiRuleInfo.builder()
                        .ruleNumber(rule.getRuleNumber())
                        .shortDescription(rule.getShortDescription())
                        .build())
                .collect(Collectors.toList());
        
        log.debug("Found sandhiRuleInfos: {}", sandhiRuleInfos);

        return SolutionDto.builder()
                .id(solution.getId())
                .solutionText(solution.getSolutionText())
                .stepByStep(solution.getStepByStep())
                .sandhiRules(sandhiRuleInfos)
                .build();
    }

    private EamenauExerciseDto mapToDto(Exercise exercise) {
        return EamenauExerciseDto.builder()
                .id(exercise.getId())
                .exerciseNumber(exercise.getExerciseNumber())
                .exerciseLetter(exercise.getExerciseLetter())
                .instructionText(exercise.getInstructionText())
                .build();
    }

    private EamenauExerciseDetailDto mapToDetailDto(Exercise exercise) {
        List<Task> tasks = taskRepository.findByExerciseIdOrderByTaskNumberAsc(exercise.getId());
        return EamenauExerciseDetailDto.builder()
                .id(exercise.getId())
                .exerciseNumber(exercise.getExerciseNumber())
                .exerciseLetter(exercise.getExerciseLetter())
                .instructionText(exercise.getInstructionText())
                .tasks(tasks.stream().map(this::mapToTaskDto).collect(Collectors.toList()))
                .build();
    }

    private EamenauTaskDto mapToTaskDto(Task task) {
        return EamenauTaskDto.builder()
                .id(task.getId())
                .taskNumber(task.getTaskNumber())
                .taskText(task.getTaskText())
                .build();
    }
}
