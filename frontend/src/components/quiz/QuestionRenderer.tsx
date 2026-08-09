import type { QuestionAnswerPayload, SessionQuestion } from '../../types/quiz';
import SingleChoiceQuestion from './SingleChoiceQuestion';
import FreeTextQuestion from './FreeTextQuestion';
import MatchingQuestion from './MatchingQuestion';

export interface QuestionRendererProps {
  question: SessionQuestion;
  disabled?: boolean;
  feedback?: unknown;
  selectedOptionId?: string | null;
  currentQuestionIndex: number;
  totalQuestions: number;
  /** Доменные данные ответа (id опции / строка / MatchingAnswerPayload). */
  onSubmit: (answer: QuestionAnswerPayload) => void;
}

/**
 * Тонкий диспетчер: рендерит компонент по `answerMode`, никакой логики проверки
 * ответа здесь нет (task-frontend-01 §2).
 */
export default function QuestionRenderer({
  question,
  disabled,
  feedback,
  selectedOptionId,
  currentQuestionIndex,
  totalQuestions,
  onSubmit,
}: QuestionRendererProps) {
  switch (question.answerMode) {
    case 'FREE_TEXT':
      return (
        <FreeTextQuestion
          question={question}
          disabled={disabled}
          feedback={feedback}
          currentQuestionIndex={currentQuestionIndex}
          totalQuestions={totalQuestions}
          onSubmit={(value) => onSubmit(value)}
        />
      );
    case 'MATCHING':
      return (
        <MatchingQuestion
          question={question}
          disabled={disabled}
          feedback={feedback}
          onSubmit={(payload) => onSubmit(payload)}
        />
      );
    case 'MULTI_SELECT':
    case 'SINGLE_CHOICE':
    case 'SPAN_SELECT':
    default:
      return (
        <SingleChoiceQuestion
          question={question}
          disabled={disabled}
          feedback={feedback}
          selectedOptionId={selectedOptionId ?? null}
          onSubmit={(optionId) => onSubmit(optionId)}
        />
      );
  }
}
