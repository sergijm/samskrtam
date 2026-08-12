package sm.selflearn.samskrtam.quiz.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import sm.selflearn.samskrtam.content.dto.GeneratedQuizQuestionDto;
import sm.selflearn.samskrtam.content.dto.QuestionLanguage;
import sm.selflearn.samskrtam.content.dto.VocabularyWordDto;
import sm.selflearn.samskrtam.quiz.dto.AnswerRequest;

import java.util.List;
import java.util.UUID;

/**
 * Поиск словарных ответов — resolve правильного/выбранного варианта по словарю.
 * Выделен из {@link QuizDataAssembler} для компактности.
 */
@Component
@RequiredArgsConstructor
public class VocabularyAnswerResolver {

    /**
     * Определяет IAST выбранного варианта ответа.
     * Для vocabulary-вопросов ищет слово по ID, для declension берёт formIast из запроса.
     */
    public String determineSelectedOptionIast(
            AnswerRequest request,
            GeneratedQuizQuestionDto generatedQuestion,
            List<VocabularyWordDto> allVocabularyWords) {

        if (generatedQuestion.getVocabularyWordId() != null) {
            VocabularyWordDto selectedWord = allVocabularyWords.stream()
                    .filter(w -> w.getId().equals(request.getSelectedOptionId()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException(
                            "Selected vocabulary word not found: " + request.getSelectedOptionId()));

            if (generatedQuestion.getQuestionTargetLanguage() == QuestionLanguage.SANSKRIT) {
                return selectedWord.getWordIast();
            } else if (generatedQuestion.getQuestionTargetLanguage() == QuestionLanguage.RUSSIAN) {
                return selectedWord.getTranslationRu();
            } else if (generatedQuestion.getQuestionTargetLanguage() == QuestionLanguage.ENGLISH) {
                return selectedWord.getTranslationEn();
            } else {
                return null;
            }
        } else {
            return request.getSelectedFormIast();
        }
    }

    /**
     * Находит ID правильного словарного слова (null, если это declension-вопрос).
     */
    public UUID findCorrectWordId(
            GeneratedQuizQuestionDto generatedQuestion,
            List<VocabularyWordDto> allVocabularyWords) {

        if (generatedQuestion.getVocabularyWordId() != null) {
            return allVocabularyWords.stream()
                    .filter(w -> w.getId().equals(generatedQuestion.getVocabularyWordId()))
                    .map(VocabularyWordDto::getId)
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }

    /**
     * Находит текст правильного ответа: перевод слова или IAST-форму склонения.
     */
    public String findCorrectAnswerText(
            GeneratedQuizQuestionDto generatedQuestion,
            List<VocabularyWordDto> allVocabularyWords,
            String userLocale) {

        if (generatedQuestion.getVocabularyWordId() != null) {
            VocabularyWordDto correctWord = allVocabularyWords.stream()
                    .filter(w -> w.getId().equals(generatedQuestion.getVocabularyWordId()))
                    .findFirst()
                    .orElse(null);

            if (correctWord != null) {
                if (generatedQuestion.getQuestionTargetLanguage() == QuestionLanguage.SANSKRIT) {
                    return correctWord.getWordIast();
                } else if (generatedQuestion.getQuestionTargetLanguage() == QuestionLanguage.RUSSIAN) {
                    return correctWord.getTranslationRu();
                } else if (generatedQuestion.getQuestionTargetLanguage() == QuestionLanguage.ENGLISH) {
                    return correctWord.getTranslationEn();
                }
            }
        }
        return generatedQuestion.getCorrectFormIast();
    }
}
