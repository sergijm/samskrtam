import type { HighlightToken } from '../../types/quiz';
import { promptText } from '../../utils/grammarTerms';

interface HighlightedPromptProps {
  question: { text: string; textRu?: string; highlights?: HighlightToken[] };
  lang: string;
}

/**
 * Рендерит промпт вопроса, выделяя жирным слова из `highlights`.
 * Токен выбирается по языку интерфейса (textRu для ru, text — для остальных);
 * split по токенам выполняется без regex-меток (токен экранируется).
 */
export default function HighlightedPrompt({ question, lang }: HighlightedPromptProps) {
  const text = promptText(question, lang);
  const tokens =
    question.highlights
      ?.map((h) => (lang === 'ru' ? h.textRu || h.text : h.text))
      .filter((t): t is string => !!t && t.length > 0) ?? [];

  if (tokens.length === 0) {
    return <>{text}</>;
  }

  const escaped = tokens
    .map((t) => t.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'))
    .sort((a, b) => b.length - a.length);
  const pattern = new RegExp(`(${escaped.join('|')})`, 'g');
  const parts = text.split(pattern);
  const tokenSet = new Set(tokens);

  return (
    <>
      {parts.map((part, i) =>
        tokenSet.has(part) ? <strong key={i}>{part}</strong> : <span key={i}>{part}</span>,
      )}
    </>
  );
}