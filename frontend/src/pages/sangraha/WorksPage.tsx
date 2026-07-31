import { useTranslation } from 'react-i18next';
import { Skeleton } from 'primereact/skeleton';
import { useNavigate } from 'react-router-dom';

import { useWorks } from '../../hooks/useSangraha';
import type { WorkSummaryDto } from '../../types/sangraha';

const WorksPage = () => {
  const { t, i18n } = useTranslation();
  const navigate = useNavigate();
  const { data: works, isLoading, isError } = useWorks();

  if (isLoading) {
    return (
      <div className="p-4">
        {[1, 2, 3, 4, 5].map((i) => (
          <Skeleton key={i} width="100%" height="1.5rem" className="mb-2" />
        ))}
      </div>
    );
  }

  if (isError) {
    return (
      <div className="p-4">
        <div className="p-error">{t('common.error')}</div>
      </div>
    );
  }

  return (
    <div className="p-4">
      <h1 className="mb-3">{t('sangraha.works')}</h1>

      {works && works.length > 0 ? (
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
                    {i18n.language === 'ru' ? (work.titleRu || work.titleEn) : work.titleEn}
                  </span>
                  <span className="text-xs text-color-secondary font-italic">
                    {i18n.language === 'ru' ? work.descriptionRu : work.descriptionEn}
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

