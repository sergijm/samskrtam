package sm.selflearn.samskrtam.content.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Назначает questionType для вопросов квиза по склонениям.
 * Чистый резолвер: история предыдущих типов + доступность ENDING_MATCH → тип.
 */
@Component
public class DeclensionQuestionTypeResolver {

    private static final Random RANDOM = new Random();

    /**
     * @param previousQuestionTypesInSession список уже назначенных questionType в этой сессии (по порядку questionNumber)
     * @param endingHasEnoughHomonyms        true если окончание имеет ≥2 омонимичных троек (vowel_type, endingIast)
     * @return один из "FORM_BY_CASE", "CASE_BY_FORM", "ENDING_MATCH"
     */
    public String resolveQuestionType(List<String> previousQuestionTypesInSession, boolean endingHasEnoughHomonyms) {
        // Собираем пул кандидатов (дубликаты = "вес")
        List<String> pool = new ArrayList<>(List.of(
                "FORM_BY_CASE", "FORM_BY_CASE", "FORM_BY_CASE", "FORM_BY_CASE", "FORM_BY_CASE",
                "CASE_BY_FORM", "CASE_BY_FORM", "CASE_BY_FORM"
        ));
        //if (endingHasEnoughHomonyms) {
            //pool.add("ENDING_MATCH");
            //pool.add("ENDING_MATCH");
        //} else {
            pool.add("CASE_BY_FORM");
            pool.add("CASE_BY_FORM");
        //}

        // Определяем lastTwo — последние 2 элемента истории
        List<String> lastTwo = previousQuestionTypesInSession.size() >= 2
                ? previousQuestionTypesInSession.subList(previousQuestionTypesInSession.size() - 2, previousQuestionTypesInSession.size())
                : List.of();

        // До 10 попыток — антиповтор двух одинаковых подряд
        String candidate = null;
        for (int attempt = 0; attempt < 10; attempt++) {
            candidate = pool.get(RANDOM.nextInt(pool.size()));
            boolean isRepeat = lastTwo.size() == 2
                    && lastTwo.get(0).equals(candidate)
                    && lastTwo.get(1).equals(candidate);
            if (!isRepeat) {
                return candidate;
            }
        }
        // Крайний случай — не зацикливаемся
        return candidate;
    }
}
