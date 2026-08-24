import { useMemo, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { Skeleton } from 'primereact/skeleton';
import { lessonApi } from '../../api/lessonApi';
import CaseEndingsTable from '../../components/lesson/CaseEndingsTable';
import CaseEndingsFilters, { ColumnFilterDef } from '../../components/lesson/CaseEndingsFilters';
import { normalizeDiacritics, truncateEnding } from '../../utils/diacritics';
import type { CaseEndingDto } from '../../types/content-dtos';

const FILTER_COLUMNS: ColumnFilterDef[] = [
  { key: 'stemType', labelKey: 'caseEndings.stemType', section: 'stemType', groupLabelKey: 'caseEndings.stemGroup' },
  { key: 'gender', labelKey: 'caseEndings.gender', section: 'gender', row: 'gn' },
  { key: 'number', labelKey: 'caseEndings.number', section: 'number', row: 'gn' },
  { key: 'grammaticalCase', labelKey: 'caseEndings.grammaticalCase', section: 'case' },
  { key: 'caseEnding', labelKey: 'caseEndings.caseEnding', normalize: true },
];

const CaseEndingsLessonPage = () => {
  const { t, i18n } = useTranslation();
  const { data, isLoading, isError } = useQuery<CaseEndingDto[]>({
    queryKey: ['case-endings'],
    queryFn: () => lessonApi.getCaseEndings().then((res) => res.data),
  });

  const [selected, setSelected] = useState<Record<string, Set<string>>>({});

  const filtered = useMemo(() => {
    if (!data) return [];
    return data.filter((row) =>
      FILTER_COLUMNS.every((col) => {
        const set = selected[col.key];
        if (!set || set.size === 0) return true;
        const cell = col.normalize
          ? truncateEnding(normalizeDiacritics(String(row[col.key])))
          : String(row[col.key]);
        return set.has(cell);
      }),
    );
  }, [data, selected]);

  const toggle = (columnKey: string, value: string) => {
    setSelected((prev) => {
      const next = { ...prev };
      const cur = new Set(next[columnKey] ?? []);
      if (cur.has(value)) cur.delete(value);
      else cur.add(value);
      next[columnKey] = cur;
      return next;
    });
  };
  const clearGroup = (columnKey: string) =>
    setSelected((prev) => {
      const next = { ...prev };
      delete next[columnKey];
      return next;
    });
  const clearAll = () => setSelected({});

  if (isError) {
    return (
      <div className="p-4">
        <div className="p-error">{i18n.language === 'ru' ? 'Ошибка загрузки' : 'Load error'}</div>
      </div>
    );
  }

  return (
    <div className="p-4">
      <div className="card mb-3">
        <h1 className="m-0 text-2xl">{t('caseEndings.title')}</h1>
      </div>

      {isLoading || !data ? (
        <div className="card">
          <Skeleton width="100%" height="40px" className="mb-2" />
          <Skeleton width="100%" height="20px" className="mb-2" />
          <Skeleton width="100%" height="400px" />
        </div>
      ) : (
        <>
          <div className="card mb-3">
            <CaseEndingsFilters
              columns={FILTER_COLUMNS}
              rows={data}
              selected={selected}
              onToggle={toggle}
              onClearGroup={clearGroup}
              onClearAll={clearAll}
            />
          </div>
          <div className="card">
            <CaseEndingsTable rows={filtered} />
          </div>
        </>
      )}
    </div>
  );
};

export default CaseEndingsLessonPage;
