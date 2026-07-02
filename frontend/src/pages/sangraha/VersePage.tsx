import { useTranslation } from 'react-i18next';
import { useParams, useNavigate } from 'react-router-dom';
import {
  useVerseDetail,
  useUpdateVerseText,
  useAnalyzeVerse,
} from '../../hooks/useSangraha';
import { useAuthStore } from '../../store/authStore';
import { Button } from 'primereact/button';
import { InputTextarea } from 'primereact/inputtextarea';
import { DataTable } from 'primereact/datatable';
import { Column } from 'primereact/column';
import { Tag } from 'primereact/tag';
import { Toast } from 'primereact/toast';
import { Skeleton } from 'primereact/skeleton';
import { Panel } from 'primereact/panel';
import { Divider } from 'primereact/divider';
import { useRef, useState, useCallback } from 'react';
import type { VerseWordDto, SandhiSplit } from '../../types/sangraha';

const posLabels: Record<string, string> = {
  NOUN: 'noun', VERB: 'verb', ADJECTIVE: 'adj', PRONOUN: 'pron', INDECLINABLE: 'indecl', NUMERAL: 'num',
};

const VersePage = () => {
  const { t } = useTranslation();
  const { workSlug, verseId } = useParams<{ workSlug: string; verseId: string }>();
  const navigate = useNavigate();
  const toast = useRef<Toast>(null);
  const { data: verse, isLoading, isError } = useVerseDetail(verseId || '');
  const updateText = useUpdateVerseText();
  const analyze = useAnalyzeVerse();
  const user = useAuthStore((s) => s.user);
  const isAdmin = user?.roles?.includes('ADMIN') ?? false;

  const [isEditing, setIsEditing] = useState(!verse || verse.status === 'DRAFT' || verse.status === 'FAILED');
  const [editDevanagari, setEditDevanagari] = useState(verse?.textDevanagari || '');
  const [editIast, setEditIast] = useState(verse?.textIast || '');

  const handleSaveText = useCallback(async () => {
    if (!verseId) return;
    try {
      await updateText.mutateAsync({ verseId, data: { textDevanagari: editDevanagari, textIast: editIast } });
      toast.current?.show({ severity: 'success', summary: t('common.saved') });
    } catch {
      toast.current?.show({ severity: 'error', summary: t('common.error') });
    }
  }, [verseId, editDevanagari, editIast, updateText, t]);

  const handleAnalyze = useCallback(async () => {
    if (!verseId) return;
    try {
      await analyze.mutateAsync(verseId);
      setIsEditing(false);
      toast.current?.show({ severity: 'success', summary: t('sangraha.action.analyze') });
    } catch {
      toast.current?.show({ severity: 'error', summary: t('common.error') });
    }
  }, [verseId, analyze, t]);

  const isDraft = verse?.status === 'DRAFT' || verse?.status === 'FAILED';
  const isAnalyzed = verse?.status === 'ANALYZED';
  const isAnalyzing = verse?.status === 'ANALYZING';

  if (isError) {
    return (
      <div className="p-4">
        <div className="p-error">{t('common.error')}</div>
      </div>
    );
  }

  if (isLoading || !verse) {
    return (
      <div className="p-4">
        <Skeleton width="100%" height="30px" className="mb-3" />
        <Skeleton width="100%" height="200px" className="mb-3" />
        <Skeleton width="100%" height="300px" />
      </div>
    );
  }

  return (
    <div className="p-4">
      <Toast ref={toast} />
      <div className="flex align-items-center mb-3">
        <Button
          icon="pi pi-arrow-left"
          className="p-button-text p-button-rounded mr-2"
          onClick={() => navigate(`/sangraha/${workSlug}`)}
        />
        <h2 className="m-0">{t('sangraha.verse')} #{verse.orderIndex}</h2>
        <Tag
          value={t(`sangraha.status.${verse.status}`)}
          severity={verse.status === 'ANALYZED' ? 'success' : verse.status === 'FAILED' ? 'danger' : 'warn'}
          className="ml-2"
        />
      </div>

      {/* DRAFT / EDIT mode */}
      {(isEditing || isDraft) && (
        <div className="mb-4">
          <Panel header={t('sangraha.fields.textDevanagari')}>
            <InputTextarea
              value={editDevanagari}
              onChange={(e) => setEditDevanagari(e.target.value)}
              className="w-full"
              rows={4}
              placeholder={t('sangraha.placeholder.textDevanagari')}
            />
          </Panel>
          <Panel header={t('sangraha.fields.textIast')} className="mt-2">
            <InputTextarea
              value={editIast}
              onChange={(e) => setEditIast(e.target.value)}
              className="w-full"
              rows={4}
              placeholder={t('sangraha.placeholder.textIast')}
            />
          </Panel>
          {isAdmin && !isAnalyzing && (
            <div className="flex gap-2 mt-3">
              <Button
                label={t('sangraha.action.save')}
                icon="pi pi-save"
                onClick={handleSaveText}
                loading={updateText.isPending}
              />
              <Button
                label={t('sangraha.action.analyze')}
                icon="pi pi-robot"
                className="p-button-success"
                onClick={handleAnalyze}
                loading={analyze.isPending}
              />
            </div>
          )}
          {isAnalyzing && (
            <div className="mt-3">
              <Skeleton width="100%" height="50px" />
              <p className="mt-2 text-color-secondary">{t('sangraha.status.ANALYZING')}</p>
            </div>
          )}
        </div>
      )}

      {/* ANALYZED read-only mode */}
      {isAnalyzed && verse.analysis && (
        <div>
          {/* Full devanagari & IAST */}
          <Panel header="Devanāgarī" className="mb-2">
            <p className="text-xl font-bold">{verse.textDevanagari}</p>
          </Panel>
          <Panel header="IAST" className="mb-2">
            <p className="text-lg">{verse.textIast}</p>
          </Panel>

          {/* Translation */}
          <div className="grid mb-3">
            <div className="col-12 md:col-6">
              <Panel header={t('sangraha.translation.ru')}>
                <p>{verse.analysis.translationRu}</p>
              </Panel>
            </div>
            <div className="col-12 md:col-6">
              <Panel header={t('sangraha.translation.en')}>
                <p>{verse.analysis.translationEn}</p>
              </Panel>
            </div>
          </div>

          {/* Sandhi Splits */}
          <Panel header={t('sangraha.sandhiSplits')} className="mb-3">
            <div className="flex flex-wrap gap-3">
              {(verse.analysis.sandhiSplits || []).map((split: SandhiSplit, idx: number) => (
                <div key={idx} className="flex align-items-center gap-1">
                  <span className="font-bold">{split.surface}</span>
                  <span className="text-color-secondary">{'<'}</span>
                  <span className="text-sm">{split.components.join(' + ')}</span>
                  <span className="text-color-secondary">{'>'}</span>
                </div>
              ))}
            </div>
          </Panel>

          <Divider />

          {/* Words Table */}
          <Panel header={t('sangraha.verses')}>
            <DataTable value={verse.words || []} responsiveLayout="scroll" size="small">
              <Column field="position" header={t('sangraha.wordsTable.position')} style={{ width: '50px' }} />
              <Column
                header={t('sangraha.wordsTable.surface')}
                body={(row: VerseWordDto) => (
                  <div>
                    <div className="font-bold">{row.surfaceDevanagari}</div>
                    <div className="text-sm text-color-secondary">{row.surfaceIast}</div>
                  </div>
                )}
              />
              <Column
                header={t('sangraha.wordsTable.lemma')}
                body={(row: VerseWordDto) => (
                  <div>
                    <div>{row.lemmaIast}</div>
                    {row.root && <div className="text-xs">√{row.root}</div>}
                  </div>
                )}
              />
              <Column field="stem" header={t('sangraha.wordsTable.stem')} />
              <Column
                field="pos"
                header={t('sangraha.wordsTable.pos')}
                body={(row: VerseWordDto) => row.pos ? t(posLabels[row.pos] || row.pos) : '-'}
              />
              <Column field="gender" header={t('sangraha.wordsTable.gender')} />
              <Column field="caseType" header={t('sangraha.wordsTable.case')} />
              <Column field="numberType" header={t('sangraha.wordsTable.number')} />
              <Column field="person" header={t('sangraha.wordsTable.person')} />
              <Column field="tense" header={t('sangraha.wordsTable.tense')} />
              <Column field="mood" header={t('sangraha.wordsTable.mood')} />
              <Column field="voice" header={t('sangraha.wordsTable.voice')} />
              <Column
                header={t('sangraha.wordsTable.gloss')}
                body={(row: VerseWordDto) => (
                  <div>
                    <div className="text-sm">{row.glossRu}</div>
                    <div className="text-xs text-color-secondary">{row.glossEn}</div>
                  </div>
                )}
              />
            </DataTable>
          </Panel>

          {/* Edit button */}
          {isAdmin && (
            <div className="mt-3">
              <Button
                label={t('sangraha.action.edit')}
                icon="pi pi-pencil"
                onClick={() => {
                  setEditDevanagari(verse.textDevanagari || '');
                  setEditIast(verse.textIast || '');
                  setIsEditing(true);
                }}
              />
            </div>
          )}
        </div>
      )}
    </div>
  );
};

export default VersePage;
