import { useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Skeleton } from 'primereact/skeleton';
import { TreeSelect } from 'primereact/treeselect';
import type { TreeNode } from 'primereact/treenode';
import { useNavigate } from 'react-router-dom';

import { useWorks, useWorksClasses } from '../../hooks/useSangraha';
import type { WorksClassTreeNodeDto } from '../../types/sangraha';

const toTreeNode = (node: WorksClassTreeNodeDto, lang: string): TreeNode => ({
  key: node.id,
  label: lang === 'ru' ? node.titleRu : node.titleEn,
  children: node.children.map((child) => toTreeNode(child, lang)),
  leaf: node.children.length === 0,
});

type SelectionMap = Record<string, Record<string, boolean>>;

/**
 * Нормализует value TreeSelect (multiple/checkbox) к плоскому списку выбранных
 * ключей.  value — объект вида { key: true } или { key: { checked: true } }.
 */
const normalizeSelection = (value: unknown): Record<string, boolean> => {
  const out: Record<string, boolean> = {};
  if (value && typeof value === 'object') {
    for (const [k, v] of Object.entries(value as Record<string, unknown>)) {
      out[k] = v === true || (typeof v === 'object' && v !== null && (v as Record<string, unknown>).checked === true);
    }
  }
  return out;
};

const WorksPage = () => {
  const { t, i18n } = useTranslation();
  const navigate = useNavigate();
  const lang = i18n.language;

  const { data: classes, isLoading: classesLoading } = useWorksClasses();
  const [selected, setSelected] = useState<SelectionMap>({});

  const selectedIds = useMemo(
    () => Object.values(selected).flatMap((m) => Object.keys(m).filter((k) => m[k])),
    [selected],
  );

  const { data: works, isLoading, isError } = useWorks(selectedIds);

  const groupNodes = useMemo(
    () =>
      (classes ?? []).map((group) => ({
        group,
        treeNodes: group.classes.map((node) => toTreeNode(node, lang)),
      })),
    [classes, lang],
  );

  const hasAnyFilter = selectedIds.length > 0;

  return (
    <div className="p-4">
      <h1 className="mb-3">{t('sangraha.works')}</h1>

      {classesLoading ? (
        <Skeleton width="100%" height="2.5rem" className="mb-3" />
      ) : groupNodes.length > 0 ? (
        <div className="flex flex-wrap gap-2 mb-3 align-items-center">
          {groupNodes.map(({ group, treeNodes }) => (
            <TreeSelect
              key={group.classification}
              value={selected[group.classification] ?? {}}
              options={treeNodes}
              onChange={(e) =>
                setSelected((prev) => ({
                  ...prev,
                  [group.classification]: normalizeSelection(e.value),
                }))
              }
              selectionMode="checkbox"
              display="chip"
              placeholder={group.classification}
              style={{ minWidth: '16rem' }}
            />
          ))}
          {hasAnyFilter && (
            <button
              type="button"
              className="p-button-text p-button-sm"
              onClick={() => setSelected({})}
            >
              <i className="pi pi-times mr-1" />
              {t('sangraha.action.resetFilter')}
            </button>
          )}
        </div>
      ) : null}

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
                    {lang === 'ru' ? (work.titleRu || work.titleEn) : work.titleEn}
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
                <i className="pi pi-chevron-right text-color-secondary ml-2" />
              </div>
            </div>
          ))}
        </div>
      ) : (
        <div className="text-center text-color-secondary p-4">{t('sangraha.noWorks')}</div>
      )}
    </div>
  );
};

export default WorksPage;