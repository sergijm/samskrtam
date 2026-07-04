import { useTranslation } from 'react-i18next';
import { DataTable } from 'primereact/datatable';
import { Column } from 'primereact/column';
import { Panel } from 'primereact/panel';
import { Divider } from 'primereact/divider';
import { Button } from 'primereact/button';
import type { VerseWordDto, SandhiSplit } from '../../types/sangraha';

interface VerseAnalysisPanelProps {
  textDevanagari: string;
  textIast: string;
  analysis: {
    translationRu: string;
    translationEn: string;
    sandhiSplits: SandhiSplit[];
  };
  words: VerseWordDto[];
  isAdmin: boolean;
  onEdit: () => void;
}

const posLabels: Record<string, string> = {
  NOUN: 'noun', VERB: 'verb', ADJECTIVE: 'adj', PRONOUN: 'pron', INDECLINABLE: 'indecl', NUMERAL: 'num',
};

export default function VerseAnalysisPanel({
  textDevanagari,
  textIast,
  analysis,
  words,
  isAdmin,
  onEdit,
}: VerseAnalysisPanelProps) {
  const { t } = useTranslation();

  return (
    <div>
      <Panel header="Devanāgarī" className="mb-2">
        <p className="text-xl font-bold">{textDevanagari}</p>
      </Panel>
      <Panel header="IAST" className="mb-2">
        <p className="text-lg">{textIast}</p>
      </Panel>

      <div className="grid mb-3">
        <div className="col-12 md:col-6">
          <Panel header={t('sangraha.translation.ru')}>
            <p>{analysis.translationRu}</p>
          </Panel>
        </div>
        <div className="col-12 md:col-6">
          <Panel header={t('sangraha.translation.en')}>
            <p>{analysis.translationEn}</p>
          </Panel>
        </div>
      </div>

      <Panel header={t('sangraha.sandhiSplits')} className="mb-3">
        <div className="flex flex-wrap gap-3">
          {(analysis.sandhiSplits || []).map((split: SandhiSplit, idx: number) => (
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

      <Panel header={t('sangraha.verses')}>
        <DataTable value={words || []} responsiveLayout="scroll" size="small">
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
            body={(row: VerseWordDto) => (row.pos ? t(posLabels[row.pos] || row.pos) : '-')}
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

      {isAdmin && (
        <div className="mt-3">
          <Button label={t('sangraha.action.edit')} icon="pi pi-pencil" onClick={onEdit} />
        </div>
      )}
    </div>
  );
}