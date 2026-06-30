package sm.selflearn.samskrtam.content.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sm.selflearn.samskrtam.common.SamskrtamException;
import sm.selflearn.samskrtam.content.dto.*;
import sm.selflearn.samskrtam.emenau.model.*;
import sm.selflearn.samskrtam.emenau.repository.*;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EamenauExerciseService {

    private final ExerciseRepository exerciseRepository;
    private final TaskRepository taskRepository;
    private final SolutionRepository solutionRepository;
    private final SolutionSandhiRuleRepository solutionSandhiRuleRepository;
    private final SandhiRuleRepository sandhiRuleRepository;

    public List<EmenauExerciseDto> getAllExercises() {
        return exerciseRepository.findAllByOrderByExerciseNumberAscExerciseLetterAsc().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public EmenauExerciseDetailDto getExerciseById(Integer id) {
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

        List<Integer> uniqueSandhiRuleNumbersList = new ArrayList<>(uniqueSandhiRuleNumbers);

        return sandhiRuleRepository.findByRuleNumberIn(uniqueSandhiRuleNumbersList).stream()
                .map(rule -> SandhiRuleInfo.builder()
                        .ruleNumber(rule.getRuleNumber())
                        .shortDescription(rule.getShortDescription())
                        .build())
                .distinct()
                .collect(Collectors.toList());
    }

    @Transactional
    public void updateSolution(Integer solutionId, SolutionUpdateRequestDto requestDto) {
        Solution solution = solutionRepository.findById(solutionId)
                .orElseThrow(() -> new SamskrtamException("SOLUTION_NOT_FOUND", "Solution not found with id: " + solutionId));

        solution.setStepByStep(requestDto.getStepByStep());
        solutionRepository.save(solution);

        // 1. Get existing and desired rule IDs
        Set<Integer> existingRuleIds = solutionSandhiRuleRepository.findBySolutionId(solutionId).stream()
                .map(SolutionSandhiRule::getSandhiRuleId)
                .collect(Collectors.toSet());

        Set<Integer> desiredRuleIds = new HashSet<>();
        if (requestDto.getRuleNumbers() != null && !requestDto.getRuleNumbers().isBlank()) {
            List<Integer> ruleNumbers = Arrays.stream(requestDto.getRuleNumbers().split("[\\s,;]+"))
                    .filter(s -> !s.isBlank())
                    .map(Integer::parseInt)
                    .toList();
            desiredRuleIds = sandhiRuleRepository.findByRuleNumberIn(ruleNumbers).stream()
                    .map(SandhiRule::getId)
                    .collect(Collectors.toSet());
        }

        // 2. Find rules to add
        Set<Integer> rulesToAdd = new HashSet<>(desiredRuleIds);
        rulesToAdd.removeAll(existingRuleIds);

        List<SolutionSandhiRule> newRules = rulesToAdd.stream()
                .map(ruleId -> new SolutionSandhiRule(null, solutionId, ruleId))
                .collect(Collectors.toList());

        // 3. Find rules to remove
        Set<Integer> rulesToRemove = new HashSet<>(existingRuleIds);
        rulesToRemove.removeAll(desiredRuleIds);

        // 4. Perform database operations
        if (!newRules.isEmpty()) {
            solutionSandhiRuleRepository.saveAll(newRules);
        }
        if (!rulesToRemove.isEmpty()) {
            solutionSandhiRuleRepository.deleteBySolutionIdAndSandhiRuleIdIn(solutionId, new ArrayList<>(rulesToRemove));
        }
    }

    private SolutionDto mapToSolutionDto(Solution solution) {
        List<Integer> sandhiRuleNumbersFromSolution = solutionSandhiRuleRepository.findBySolutionId(solution.getId()).stream()
                .map(SolutionSandhiRule::getSandhiRuleId)
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

    private EmenauExerciseDto mapToDto(Exercise exercise) {
        return EmenauExerciseDto.builder()
                .id(exercise.getId())
                .exerciseNumber(exercise.getExerciseNumber())
                .exerciseLetter(exercise.getExerciseLetter())
                .instructionText(exercise.getInstructionText())
                .build();
    }

    private EmenauExerciseDetailDto mapToDetailDto(Exercise exercise) {
        List<Task> tasks = taskRepository.findByExerciseIdOrderByTaskNumberAsc(exercise.getId());
        return EmenauExerciseDetailDto.builder()
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
