package sm.selflearn.samskrtam.quiz.dto;

public record QuizProgressByCaseDto(
        String caseType,
        int aggregatedProgress,
        int totalCombinations,
        int learnedCombinations
) {
}