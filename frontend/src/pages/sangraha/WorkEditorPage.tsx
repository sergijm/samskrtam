import { useParams, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useState, useCallback, useEffect } from 'react';
import { InputText } from 'primereact/inputtext';
import { Toast } from 'primereact/toast';
import { useRef } from 'react';

import { useWorkTree, useCreateWork, useUpdateWork, useCreateChapter, useUpdateChapter } from '../../hooks/useSangraha';
import { IconButton, SubmitButton, PageButton } from '../../components/common/buttons';
import type { ChapterSummaryDto } from '../../types/sangraha';
import './WorkPage.css';

interface ChapterRow {
  key: string;
  id?: string;
  titleRu: string;
}

const WorkEditorPage = () => {
  const { t } = useTranslation();
  const { workSlug } = useParams<{ workSlug: string }>();
  const navigate = useNavigate();
  const toast = useRef<Toast>(null);

  const isEdit = !!workSlug;
  const { data: work, isLoading } = useWorkTree(workSlug || '');

  const [title, setTitle] = useState('');
  const [chapters, setChapters] = useState<ChapterRow[]>([]);
  const [titleSaved, setTitleSaved] = useState(false);

  const createWork = useCreateWork();
  const updateWork = useUpdateWork();
  const createChapter = useCreateChapter();
  const updateChapter = useUpdateChapter();

  // Сидим поля из загруженного произведения (режим редактирования)
  useEffect(() => {
    if (isEdit && work) {
      setTitle(work.titleRu);
      setChapters(
        (work.chapters || []).map((c: ChapterSummaryDto) => ({
          key: c.id,
          id: c.id,
          titleRu: c.titleRu,
        })),
      );
    }
  }, [isEdit, work]);

  const handleSaveTitle = useCallback(() => {
    if (!title.trim()) {
      toast.current?.show({ severity: 'warn', summary: t('sangraha.fields.title') + ' — ' + t('common.required') });
      return;
    }
    if (isEdit && workSlug) {
      updateWork.mutate(
        { workSlug, data: { titleRu: title } },
        {
          onSuccess: () => {
            setTitleSaved(true);
            toast.current?.show({ severity: 'success', summary: t('common.saved') });
          },
          onError: () => toast.current?.show({ severity: 'error', summary: t('common.error') }),
        },
      );
    } else {
      createWork.mutate(
        { titleRu: title, titleEn: title },
        {
          onSuccess: (res) => {
            setTitleSaved(true);
            toast.current?.show({ severity: 'success', summary: t('common.saved') });
            navigate(`/sangraha/${res.data.slug}/edit`, { replace: true });
          },
          onError: () => toast.current?.show({ severity: 'error', summary: t('common.error') }),
        },
      );
    }
  }, [title, isEdit, workSlug, createWork, updateWork, navigate, t]);

  const handleAddChapter = useCallback(() => {
    const slug = workSlug;
    if (!slug) return;
    setChapters((prev) => [...prev, { key: `new-${Date.now()}`, titleRu: '' }]);
  }, [workSlug]);

  const handleSaveChapter = useCallback(
    (row: ChapterRow) => {
      if (!row.titleRu.trim()) {
        toast.current?.show({ severity: 'warn', summary: t('sangraha.fields.title') + ' — ' + t('common.required') });
        return;
      }
      const slug = workSlug!;
      if (row.id) {
        updateChapter.mutate(
          { chapterId: row.id, data: { titleRu: row.titleRu } },
          {
            onSuccess: () => toast.current?.show({ severity: 'success', summary: t('common.saved') }),
            onError: () => toast.current?.show({ severity: 'error', summary: t('common.error') }),
          },
        );
      } else {
        createChapter.mutate(
          { workSlug: slug, data: { titleRu: row.titleRu } },
          {
            onSuccess: (res) => {
              setChapters((prev) =>
                prev.map((c) => (c.key === row.key ? { ...c, id: res.data.id, key: res.data.id } : c)),
              );
              toast.current?.show({ severity: 'success', summary: t('common.saved') });
            },
            onError: () => toast.current?.show({ severity: 'error', summary: t('common.error') }),
          },
        );
      }
    },
    [workSlug, createChapter, updateChapter, t],
  );

  const openChapter = useCallback(
    (row: ChapterRow) => {
      if (!row.id || !workSlug) return;
      navigate(`/sangraha/${workSlug}/chapters/${row.id}`);
    },
    [workSlug, navigate],
  );

  if (isEdit && isLoading) {
    return (
      <div className="p-4">
        <p>{t('common.loading')}</p>
      </div>
    );
  }

  return (
    <div className="p-4">
      <Toast ref={toast} />

      <div className="flex align-items-center mb-3">
        <IconButton iconName="pi-arrow-left" className="p-button-rounded mr-2" onClick={() => navigate('/sangraha')} />
        <h1 className="text-2xl font-bold m-0">
          {isEdit ? t('sangraha.editWork') : t('sangraha.createWork')}
        </h1>
      </div>

      {/* Название произведения */}
      <div className="mb-4 flex align-items-end gap-2 flex-wrap">
        <div className="flex flex-column gap-1" style={{ flex: '1 1 320px' }}>
          <label className="font-semibold">{t('sangraha.fields.title')}</label>
          <InputText
            value={title}
            onChange={(e) => {
              setTitle(e.target.value);
              setTitleSaved(false);
            }}
            placeholder={t('sangraha.placeholder.workTitle')}
          />
        </div>
        <SubmitButton
          labelKey={titleSaved ? 'sangraha.action.edit' : 'common.save'}
          loading={createWork.isPending || updateWork.isPending}
          onClick={handleSaveTitle}
        />
      </div>

      {/* Главы */}
      <div className="flex align-items-center justify-content-between mb-2">
        <h2 className="m-0">{t('sangraha.chapters')}</h2>
        <PageButton
          variant="page-action"
          labelKey="sangraha.action.addChapter"
          disabled={!workSlug}
          onClick={handleAddChapter}
        />
      </div>

      {!workSlug && (
        <p className="text-color-secondary text-sm mb-2">{t('sangraha.saveWorkFirst')}</p>
      )}

      {chapters.length === 0 ? (
        <div className="text-center text-color-secondary p-3">{t('sangraha.noChaptersYet')}</div>
      ) : (
        <div className="work-tree">
          {chapters.map((row) => (
            <div
              key={row.key}
              className="work-tree-row cursor-pointer hover:surface-hover"
              onClick={() => openChapter(row)}
            >
              <div className="work-tree-row-left" style={{ flex: 1 }}>
                <InputText
                  value={row.titleRu}
                  onClick={(e) => e.stopPropagation()}
                  onChange={(e) =>
                    setChapters((prev) =>
                      prev.map((c) => (c.key === row.key ? { ...c, titleRu: e.target.value } : c)),
                    )
                  }
                  placeholder={t('sangraha.placeholder.chapterTitle')}
                  className="w-full"
                />
              </div>
              <div className="work-tree-row-right">
                <SubmitButton
                  labelKey="common.save"
                  onClick={(e) => {
                    e.stopPropagation();
                    handleSaveChapter(row);
                  }}
                />
                <i className="pi pi-chevron-right text-color-secondary ml-2" />
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

export default WorkEditorPage;
