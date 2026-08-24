export function normalizeDiacritics(value: string): string {
  return value
    .normalize('NFD')
    .replace(/\p{M}/gu, '')
    .toLowerCase();
}

/** Collapse an ending to a compact chip label: first 3 chars + '*' when length ≥ 3. */
export function truncateEnding(normalized: string): string {
  return normalized.length >= 3 ? `${normalized.slice(0, 3)}*` : normalized;
}
