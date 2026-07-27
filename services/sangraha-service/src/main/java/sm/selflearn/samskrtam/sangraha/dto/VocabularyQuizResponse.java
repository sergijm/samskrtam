package sm.selflearn.samskrtam.sangraha.dto;

import java.util.UUID;

public record VocabularyQuizResponse(String quizSlug, UUID quizId, String quizStatus) {}
