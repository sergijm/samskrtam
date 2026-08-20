// =============================================
// Grammar terms: full and abbreviated forms
// =============================================

export const FULL_CASE: Record<string, string> = {
  'NOMINATIVE': 'Nominative',
  'ACCUSATIVE': 'Accusative',
  'INSTRUMENTAL': 'Instrumental',
  'DATIVE': 'Dative',
  'ABLATIVE': 'Ablative',
  'GENITIVE': 'Genitive',
  'LOCATIVE': 'Locative',
  'VOCATIVE': 'Vocative',
};

export const FULL_CASE_RU: Record<string, string> = {
  'NOMINATIVE': 'Именительный',
  'ACCUSATIVE': 'Винительный',
  'INSTRUMENTAL': 'Творительный',
  'DATIVE': 'Дательный',
  'ABLATIVE': 'Отложительный',
  'GENITIVE': 'Родительный',
  'LOCATIVE': 'Местный',
  'VOCATIVE': 'Звательный',
};

export const FULL_NUMBER: Record<string, string> = {
  'SINGULAR': 'Singular',
  'DUAL': 'Dual',
  'PLURAL': 'Plural',
};

export const FULL_NUMBER_RU: Record<string, string> = {
  'SINGULAR': 'Единственное',
  'DUAL': 'Двойственное',
  'PLURAL': 'Множественное',
};

export const ABBR_CASE: Record<string, string> = {
  'NOMINATIVE': 'Nom.',
  'ACCUSATIVE': 'Acc.',
  'INSTRUMENTAL': 'Ins.',
  'DATIVE': 'Dat.',
  'ABLATIVE': 'Abl.',
  'GENITIVE': 'Gen.',
  'LOCATIVE': 'Loc.',
  'VOCATIVE': 'Voc.',
};

export const ABBR_CASE_RU: Record<string, string> = {
  'NOMINATIVE': 'Им.',
  'ACCUSATIVE': 'Вин.',
  'INSTRUMENTAL': 'Тв.',
  'DATIVE': 'Дат.',
  'ABLATIVE': 'Отл.',
  'GENITIVE': 'Род.',
  'LOCATIVE': 'Мест.',
  'VOCATIVE': 'Зв.',
};

export const ABBR_NUMBER: Record<string, string> = {
  'SINGULAR': 'Sg.',
  'DUAL': 'Du.',
  'PLURAL': 'Pl.',
};

/** Lookup helper: returns value from map, or the original if not found */
export function lookup(value: string | undefined | null, map: Record<string, string>): string | null {
  if (!value) return null;
  return map[value] ?? value;
}

/** Question prompt text for a UI language: Russian variant when available, English as fallback. */
export function promptText(
  question: { text: string; textRu?: string },
  lang: string,
): string {
  return lang === 'ru' ? (question.textRu || question.text) : question.text;
}

/** Option label for a UI language: Russian variant when available, English text as fallback. */
export function optionText(
  option: { formIast: string; textRu?: string },
  lang: string,
): string {
  return lang === 'ru' ? (option.textRu || option.formIast) : option.formIast;
}

