import { useParams, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useWorkTree } from '../../hooks/useSangraha';
import { Toast } from 'primereact/toast';
import { Skeleton } from 'primereact/skeleton';
import { useRef } from 'react';

import ChapterTreeBrowser from '../../components/sangraha/ChapterTreeBrowser';
import { IconButton } from '../../components/common/buttons';
import './WorkPage.css';

const WorkPage = () => {
  const { t } = useTranslation();
  const { workSlug } = useParams<{ workSlug: string }>();
  const navigate = useNavigate();
  const toast = useRef<Toast>(null);
  const { data: work, isLoading, isError } = useWorkTree(workSlug || '');
  if (isLoading) {
    return (
      <div className="p-4">
        <Skeleton width="60%" height="2rem" className="mb-2" />
        <Skeleton width="100%" height="1.5rem" className="mb-1" />
        <Skeleton width="100%" height="1.5rem" className="mb-1" />
        <Skeleton width="100%" height="1.5rem" />
      </div>
    );
  }

  if (isError || !work) {
    return (
      <div className="p-4 text-center">
        <i className="pi pi-exclamation-triangle text-4xl text-red-500 mb-3" />
        <h3>{t('common.error')}</h3>
        <p>{t('sangraha.workNotFound')}</p>
        <IconButton iconName="pi-arrow-left" className="p-button-rounded" onClick={() => navigate('/sangraha')} />
      </div>
    );
  }

  return (
    <div className="p-4">
      <Toast ref={toast} />

      <div className="flex align-items-center justify-content-between mb-3">
        <div className="flex align-items-center gap-3">
          <IconButton iconName="pi-arrow-left" className="p-button-rounded" onClick={() => navigate('/sangraha')} />
          <h1 className="text-2xl font-bold m-0">{work.titleEn || work.titleRu}</h1>
        </div>
      </div>

      {work.descriptionEn && (
        <p className="text-color-secondary mb-4">{work.descriptionEn}</p>
      )}

            <ChapterTreeBrowser
        chapters={work.chapters || []}
        workSlug={workSlug || ''}
      />
    </div>
  );
};

export default WorkPage;