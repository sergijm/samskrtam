package sm.selflearn.samskrtam.quiz.dto;

public record QuizProgressByNumberDto(
        String numberType,
        int aggregatedProgress,
        int totalCombinations,
        int learnedCombinations
) {
}
