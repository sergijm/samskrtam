import React from 'react';
import { useTranslation } from 'react-i18next';
import type { VerbalEndingDto } from '../../types/content-dtos';
import { normalizeDiacritics, truncateEnding } from '../../utils/diacritics';

export interface ColumnFilterDef {
  key: keyof VerbalEndingDto;
  labelKey: string;
  section?: string;
  normalize?: boolean;
  groupLabelKey?: string;
  row?: string;
}

interface VerbalEndingsFiltersProps {
  columns: ColumnFilterDef[];
  rows: VerbalEndingDto[];
  selected: Record<string, Set<string>>;
  onToggle: (columnKey: string, value: string) => void;
  onClearGroup: (columnKey: string) => void;
  onClearAll: () => void;
}

const VerbalEndingsFilters: React.FC<VerbalEndingsFiltersProps> = ({
  columns,
  rows,
  selected,
  onToggle,
  onClearGroup,
  onClearAll,
}) => {
  const { t, i18n } = useTranslation();

  const valueLabel = (col: ColumnFilterDef, value: string): string =>
    col.section ? t(`${col.section}.${value}`, value) : value;

  const hasAnySelection = columns.some((col) => (selected[col.key]?.size ?? 0) > 0);

  const renderColumn = (col: ColumnFilterDef): JSX.Element => {
    const chips: { key: string; label: string }[] = [];
    if (col.normalize) {
      const groups = new Map<string, string[]>();
      rows.forEach((r) => {
        const full = normalizeDiacritics(String(r[col.key]));
        const prefix = truncateEnding(full);
        const members = groups.get(prefix) ?? [];
        if (!members.includes(full)) members.push(full);
        groups.set(prefix, members);
      });
      chips.push(
        ...Array.from(groups.entries()).map(([prefix, members]) => ({
          key: prefix,
          label: members.length === 1 ? members[0] : prefix,
        })),
      );
      chips.sort((a, b) => a.key.localeCompare(b.key));
    } else {
      const values = Array.from(new Set(rows.map((r) => String(r[col.key])))).sort((a, b) =>
        a.localeCompare(b),
      );
      values.forEach((v) => chips.push({ key: v, label: valueLabel(col, v) }));
    }
    const sel = selected[col.key] ?? new Set<string>();

    const selectedStem = selected['tenseMood'];
    const stemActive = selectedStem && selectedStem.size > 0;

    return (
      <div
        key={col.key}
        className="verbal-endings-filter-group flex align-items-center flex-wrap gap-2"
      >
        {col.groupLabelKey && (
          <span className="verbal-endings-group-label">{t(col.groupLabelKey)}</span>
        )}
        <div className="verbal-endings-chips">
          {chips.map((c) => (
            <button
              type="button"
              key={c.key}
              className={`verbal-endings-chip${
                sel.has(c.key) ? ' active' : ''
              }`}
              onClick={() => onToggle(col.key, c.key)}
            >
              {c.label}
            </button>
          ))}
          {sel.size > 0 && (
            <button
              type="button"
              className="verbal-endings-clear-btn ml-1"
              onClick={() => onClearGroup(col.key)}
              aria-label={i18n.language === 'ru' ? 'Сбросить' : 'Clear'}
            >
              {i18n.language === 'ru' ? 'Сбросить' : 'Clear'}
            </button>
          )}
        </div>
      </div>
    );
  };

  const layoutGroups: ColumnFilterDef[][] = [];
  const rowIndex = new Map<string, number>();
  columns.forEach((col) => {
    const rowKey = col.row ?? `__solo__${col.key}`;
    let idx = rowIndex.get(rowKey);
    if (idx === undefined) {
      idx = layoutGroups.length;
      rowIndex.set(rowKey, idx);
      layoutGroups.push([]);
    }
    layoutGroups[idx].push(col);
  });

  return (
    <div className="verbal-endings-filters">
      {hasAnySelection && (
        <div className="flex justify-content-end mb-2">
          <button type="button" className="verbal-endings-clear-btn" onClick={onClearAll}>
            {i18n.language === 'ru' ? 'Сбросить всё' : 'Clear all'}
          </button>
        </div>
      )}

      {layoutGroups.map((group, gi) => (
        <div
          key={gi}
          className="verbal-endings-filter-row flex flex-wrap align-items-center gap-5 mb-2"
        >
          {group.map((col) => renderColumn(col))}
        </div>
      ))}
    </div>
  );
};

export default VerbalEndingsFilters;