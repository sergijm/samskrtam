import { useTranslation } from 'react-i18next';
import { CtaButton } from '../common/buttons';

interface QuizFeedbackPanelProps {
  isCorrect: boolean;
  correctAnswerText: string;
  explanation: string;
  isLastQuestion: boolean;
  onNext: () => void;
}

export default function QuizFeedbackPanel({
  isCorrect,
  correctAnswerText,
  explanation,
  isLastQuestion,
  onNext,
}: QuizFeedbackPanelProps) {
  const { t } = useTranslation();

  return (
    <div
      className="feedback-section mt-5 p-3 border-round-md"
      style={{ backgroundColor: isCorrect ? '#e6ffe6' : '#ffe6e6' }}
    >
      <h3
        className="text-xl font-bold mb-2"
        style={{ color: isCorrect ? '#28a745' : '#dc3545' }}
      >
        {isCorrect ? t('quiz.correct') : t('quiz.incorrect')}
      </h3>
      {!isCorrect && (
        <p className="text-lg">
          {t('quiz.correctAnswerIs')}: <strong>{correctAnswerText}</strong>
        </p>
      )}
      {explanation && <p className="mt-2">{explanation}</p>}
      <CtaButton
        labelKey={isLastQuestion ? 'quiz.completeQuiz' : 'quiz.next'}
        className="mt-3 w-full"
        onClick={onNext}
      />
    </div>
  );
}