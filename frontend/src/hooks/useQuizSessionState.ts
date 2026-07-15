import { useState, useMemo } from 'react';
import type { SessionQuestion, StartOrResumeResponse } from '../types/quiz';

/* ---------- feedback ---------- */

export interface FeedbackState {
  isCorrect: boolean;
  correctOptionId: string;
  correctAnswerText: string;
  explanation: string;
}

/* ---------- public contract ---------- */

export interface QuizSessionState {
  sessionId: string | null;
  setSessionId: React.Dispatch<React.SetStateAction<string | null>>;
  currentQuestionIndex: number;
  setCurrentQuestionIndex: React.Dispatch<React.SetStateAction<number>>;
  questions: SessionQuestion[];
  setQuestions: React.Dispatch<React.SetStateAction<SessionQuestion[]>>;
  selectedOptionId: string | null;
  setSelectedOptionId: React.Dispatch<React.SetStateAction<string | null>>;
  feedback: FeedbackState | null;
  setFeedback: React.Dispatch<React.SetStateAction<FeedbackState | null>>;
  startTime: number;
  setStartTime: React.Dispatch<React.SetStateAction<number>>;
  hasAttemptedSessionLoad: boolean;
  setHasAttemptedSessionLoad: React.Dispatch<React.SetStateAction<boolean>>;
  sessionCompletionAttempted: boolean;
  setSessionCompletionAttempted: React.Dispatch<React.SetStateAction<boolean>>;
  quizSummaryData: StartOrResumeResponse | null;
  setQuizSummaryData: React.Dispatch<React.SetStateAction<StartOrResumeResponse | null>>;
  // derived
  currentQuestion: SessionQuestion | undefined;
  isLastQuestion: boolean;
}

/* ---------- hook ---------- */

export function useQuizSessionState(): QuizSessionState {
  const [sessionId, setSessionId] = useState<string | null>(null);
  const [currentQuestionIndex, setCurrentQuestionIndex] = useState(0);
  const [questions, setQuestions] = useState<SessionQuestion[]>([]);
  const [selectedOptionId, setSelectedOptionId] = useState<string | null>(null);
  const [feedback, setFeedback] = useState<FeedbackState | null>(null);
  const [startTime, setStartTime] = useState<number>(Date.now());
  const [hasAttemptedSessionLoad, setHasAttemptedSessionLoad] = useState(false);
  const [sessionCompletionAttempted, setSessionCompletionAttempted] = useState(false);
  const [quizSummaryData, setQuizSummaryData] = useState<StartOrResumeResponse | null>(null);

  const currentQuestion = useMemo(
    () => questions[currentQuestionIndex],
    [questions, currentQuestionIndex],
  );

  const isLastQuestion = useMemo(
    () => questions.length > 0 && currentQuestionIndex === questions.length - 1,
    [questions.length, currentQuestionIndex],
  );

  return {
    sessionId, setSessionId,
    currentQuestionIndex, setCurrentQuestionIndex,
    questions, setQuestions,
    selectedOptionId, setSelectedOptionId,
    feedback, setFeedback,
    startTime, setStartTime,
    hasAttemptedSessionLoad, setHasAttemptedSessionLoad,
    sessionCompletionAttempted, setSessionCompletionAttempted,
    quizSummaryData, setQuizSummaryData,
    currentQuestion,
    isLastQuestion,
  };
}
