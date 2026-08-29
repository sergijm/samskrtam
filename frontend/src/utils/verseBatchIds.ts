/**
 * localStorage-мост между кнопкой «Открыть все стихи» (вкладка «Примеры» урока
 * склонений, DeclensionExamplesPanel) и страницей /sangraha/verses
 * (sangraha-service/batch-verse-review.md): открыватель кладёт список verseId
 * в localStorage и переходит на /sangraha/verses без query-параметров, а
 * страница при отсутствии параметров читает их отсюда.
 */
const VERSE_BATCH_IDS_KEY = 'sangraha.verseBatchIds';

export const saveVerseBatchIds = (ids: string[]) =>
  localStorage.setItem(VERSE_BATCH_IDS_KEY, JSON.stringify(ids));

export const loadVerseBatchIds = (): string[] => {
  const raw = localStorage.getItem(VERSE_BATCH_IDS_KEY);
  if (!raw) return [];
  try {
    const parsed = JSON.parse(raw);
    return Array.isArray(parsed) ? parsed.filter((x): x is string => typeof x === 'string') : [];
  } catch {
    return [];
  }
};

export const clearVerseBatchIds = () => localStorage.removeItem(VERSE_BATCH_IDS_KEY);
