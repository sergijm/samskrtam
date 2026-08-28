import { useMemo, useState, useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import { Skeleton } from 'primereact/skeleton';
import { Dropdown } from 'primereact/dropdown';
import { useNavigate } from 'react-router-dom';

import { useWorks, useWorksClasses, useSources } from '../../hooks/useSangraha';
import type { WorksClassTreeNodeDto, SourceDto } from '../../types/sangraha';
import './WorkPage.css';

const FILTER_GROUPS = ['tradition', 'genre', 'school'];

interface ClassItemProps {
  node: WorksClassTreeNodeDto;
  isSelected: boolean;
  lang: string;
  onClick: (id: string) => void;
}

const ClassItem = ({ node, isSelected, lang, onClick }: ClassItemProps) => (
  <li
    className={`cursor-pointer px-2 py-0.5 border-round flex justify-content-between align-items-center ${isSelected ? 'bg-primary text-white' : 'hover:surface-hover'}`}
    onClick={() => onClick(node.id)}
  >
    <span>{lang === 'ru' ? node.titleRu : node.titleEn}</span>
    <span className="text-xs ml-2" style={{ opacity: isSelected ? 0.8 : 0.5 }}>
      {node.workCount}
    </span>
  </li>
);

const WorksPage = () => {
  const { t, i18n } = useTranslation();
  const navigate = useNavigate();
  const lang = i18n.language;

  const { data: classes, isLoading: classesLoading } = useWorksClasses();
  const { data: sources } = useSources();
  const [activeClassId, setActiveClassId] = useState<string | null>(null);
  const [activeSourceCode, setActiveSourceCode] = useState<string | undefined>(undefined);

  const selectedIds = useMemo(
    () => (activeClassId ? [activeClassId] : []),
    [activeClassId],
  );

  const { data: works, isLoading, isError } = useWorks(selectedIds, activeSourceCode);

  const handleClassClick = useCallback((id: string) => {
    setActiveClassId((prev) => (prev === id ? null : id));
  }, []);

  const sourceOptions = useMemo(() => {
    const opts: { label: string; value: string | undefined }[] = [
      { label: t('common.all'), value: undefined },
    ];
    (sources ?? []).forEach((s: SourceDto) => {
      opts.push({ label: s.code, value: s.code });
    });
    return opts;
  }, [sources, lang, t]);

  const filterGroups = useMemo(() => {
    if (!classes) return [];
    return FILTER_GROUPS
      .map((name) => {
        const group = classes.find(
          (g) => g.classification.toLowerCase() === name.toLowerCase(),
        );
        return group ? { classification: group.classification, classes: group.classes } : null;
      })
      .filter(Boolean) as { classification: string; classes: WorksClassTreeNodeDto[] }[];
  }, [classes]);

  return (
    <div className="p-4">
      <h1 className="mb-3">{t('sangraha.works')}</h1>

      <div style={{ display: 'grid', gridTemplateColumns: '260px 1fr', gap: '1.5rem', alignItems: 'start' }}>
        {/* Left column: filters */}
        <div>
          {classesLoading ? (
            <Skeleton width="100%" height="200px" />
          ) : filterGroups.length > 0 ? (
            <div className="flex flex-column gap-3">
              {filterGroups.map((group) => (
                <div key={group.classification} className="border-1 border-200 border-round-lg surface-card p-2">
                  <ul className="list-none p-0 m-0 flex flex-column gap-0">
                    {group.classes.map((node) => (
                      <ClassItem
                        key={node.id}
                        node={node}
                        isSelected={activeClassId === node.id}
                        lang={lang}
                        onClick={handleClassClick}
                      />
                    ))}
                  </ul>
                </div>
               ))}
             </div>
           ) : null}

          {/* Source filter (bottom of left column, AND with the rest) */}
          <div className="border-1 border-200 border-round-lg surface-card p-1 mt-2">
            <div className="text-xs font-semibold mb-1 px-1">{t('sangraha.source')}</div>
            <Dropdown
              value={activeSourceCode}
              options={sourceOptions}
              onChange={(e) => setActiveSourceCode(e.value)}
              placeholder={t('sangraha.filterBySource')}
              className="w-full"
              pt={{
                root: { style: { padding: '0' } },
                input: { style: { padding: '0.15rem 0.4rem' } },
                item: { style: { padding: '0.2rem 0.6rem' } },
                itemLabel: { style: { lineHeight: '1.1' } },
              }}
            />
          </div>
        </div>

        {/* Right column: works */}
        <div>
          {isLoading ? (
            <div>
              {[1, 2, 3, 4, 5].map((i) => (
                <Skeleton key={i} width="100%" height="1.5rem" className="mb-2" />
              ))}
            </div>
          ) : isError ? (
            <div className="p-error">{t('common.error')}</div>
          ) : works && works.length > 0 ? (
            <div className="work-tree">
              {works.map((work) => (
                <div
                  key={work.id}
                  className="work-tree-row cursor-pointer hover:surface-hover"
                  onClick={() => navigate(`/sangraha/${work.slug}`)}
                >
                  <div className="work-tree-row-left">
                    <i className="pi pi-bookmark text-primary" />
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '2px' }}>
                      <span className="font-bold">
                        {work.titleSaIast}
                        <span className="ml-1 text-color-secondary font-normal">
                          {lang === 'ru' ? ` (${work.titleRu})` : ` (${work.titleEn})`}
                        </span>
                        {work.titleSaDevanagari && (
                          <span
                            className="ml-2"
                            style={{ fontFamily: '"Noto Sans Devanagari", sans-serif' }}
                          >
                            {work.titleSaDevanagari}
                          </span>
                        )}
                      </span>
                      <span className="text-xs text-color-secondary font-italic">
                        {lang === 'ru' ? work.descriptionRu : work.descriptionEn}
                      </span>
                    </div>
                  </div>
                  <div className="work-tree-row-right">
                    {work.author && (
                      <span className="text-sm text-color-secondary">{work.author}</span>
                    )}
                    <span className="text-sm text-color-secondary ml-3 white-space-nowrap">
                      {t('sangraha.chaptersCount', { count: work.chapterCount })}
                      {' · '}
                      {t('sangraha.versesCount', { count: work.verseCount })}
                    </span>
                    <i className="pi pi-chevron-right text-color-secondary ml-2" />
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <div className="text-center text-color-secondary p-4">{t('sangraha.noWorks')}</div>
          )}
        </div>
      </div>
    </div>
  );
};

export default WorksPage;