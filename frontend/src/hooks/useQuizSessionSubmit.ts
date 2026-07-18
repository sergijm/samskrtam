import { useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import { useSubmitQuizAnswer } from './useQuiz';
import { LessonType } from '../types/quizEnums';
import type { AnswerRequest } from '../types/quiz';
import type { QuizSessionState } from './useQuizSessionState';

/**
 * Принимает ВЕСЬ QuizSessionState (raw + derived).
 * Возвращает handleSubmitAnswer / handleNextQuestion / submitAnswerMutation.
 */
export function useSubmitAnswerHandler(s: QuizSessionState) {
  const { i18n } = useTranslation();
  const submitAnswerMutation = useSubmitQuizAnswer();

  const handleSubmitAnswer = useCallback((optionIdToSubmit: string | string[]) => {
    const { sessionId, currentQuestion, quizSummaryData, startTime, questions, currentQuestionIndex } = s;
    if (!sessionId || !currentQuestion || !quizSummaryData) return;

    const isMultiSelect = currentQuestion.multiSelect === true;
    const optionIdsArray = Array.isArray(optionIdToSubmit) ? optionIdToSubmit : [optionIdToSubmit];

    if (isMultiSelect) {
      // For multi-select, store array; single option stored as string for backward compat
      s.setSelectedOptionIds(optionIdsArray);
    } else {
      const singleId = Array.isArray(optionIdToSubmit) ? optionIdToSubmit[0] : optionIdToSubmit;
      s.setSelectedOptionId(singleId);
    }

    const questionId = currentQuestion.id;
    const responseTimeMs = Date.now() - startTime;
    const selectedOption = currentQuestion.options.find((opt) => opt.id === optionIdsArray[0]);
    const selectedFormIast = selectedOption?.formIast;

    const answerRequest: AnswerRequest = {
      questionId,
      selectedOptionId: isMultiSelect ? '' : optionIdsArray[0],
      selectedFormIast: quizSummaryData.lessonType !== LessonType.VOCABULARY ? selectedFormIast : undefined,
      responseTimeMs,
      ...(isMultiSelect ? { selectedOptionIds: optionIdsArray } : {}),
    };

    submitAnswerMutation.mutate(
      {
        sessionId,
        quizIdentifier: quizSummaryData.quizId,
        lessonType: quizSummaryData.lessonType,
        answerRequest,
      },
      {
        onSuccess: (data) => {
          if (data.isCorrect) {
            s.setFeedback(null);
            s.setSelectedOptionId(null);
            s.setSelectedOptionIds([]);
            if (currentQuestionIndex < questions.length - 1) {
              s.setCurrentQuestionIndex((i) => i + 1);
            } else {
              s.setCurrentQuestionIndex(questions.length);
              s.setSessionCompletionAttempted(false);
            }
          } else {
            s.setFeedback({
              isCorrect: data.isCorrect,
              correctOptionId: data.correctOptionId,
              correctAnswerText: data.correctAnswerText,
              explanation: (i18n.language === 'ru' ? data.explanationRu : data.explanationEn) || 'No explanation',
            });
            s.setStartTime(Date.now());
          }
        },
        onError: (err) => {
          console.error('Failed to submit answer:', err);
          s.setSelectedOptionId(null);
          s.setSelectedOptionIds([]);
        },
      },
    );
  }, [s, submitAnswerMutation, i18n.language]);

  const handleNextQuestion = useCallback(() => {
    const { currentQuestionIndex, questions } = s;
    s.setFeedback(null);
    s.setSelectedOptionId(null);
    s.setSelectedOptionIds([]);
    if (currentQuestionIndex < questions.length - 1) {
      s.setCurrentQuestionIndex((i) => i + 1);
    } else {
      s.setCurrentQuestionIndex(questions.length);
      s.setSessionCompletionAttempted(false);
    }
  }, [s]);

  return { handleSubmitAnswer, handleNextQuestion, submitAnswerMutation };
}

