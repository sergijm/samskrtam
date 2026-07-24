import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { Tag } from 'primereact/tag';
import type { ChapterTreeDto, VerseTreeDto } from '../../types/sangraha';
import { IconButton } from '../common/buttons';

interface ChapterTreeBrowserProps {
  chapters: ChapterTreeDto[];
  workSlug: string;
  isAdmin: boolean;
  expandedChapters: Set<string>;
  onToggleChapter: (chapterId: string) => void;
  onAddVerse: (chapterId: string) => void;
  onDeleteChapter: (chapterId: string) => void;
  onDeleteVerse: (verseId: string) => void;
}

const statusSeverity: Record<string, 'success' | 'info' | 'warn' | 'danger'> = {
  ANALYZED: 'success',
  ANALYZING: 'info',
  DRAFT: 'warn',
  FAILED: 'danger',
};

export default function ChapterTreeBrowser({
  chapters,
  workSlug,
  isAdmin,
  expandedChapters,
  onToggleChapter,
  onAddVerse,
  onDeleteChapter,
  onDeleteVerse,
}: ChapterTreeBrowserProps) {
  const { t, i18n } = useTranslation();
  const navigate = useNavigate();

  const translationFor = (v: VerseTreeDto) =>
    i18n.language === 'ru' ? v.translationRu : v.translationEn;

  return (
    <div className="work-tree">
      {chapters?.map((ch: ChapterTreeDto) => (
        <div key={ch.id} className="work-tree-chapter">
          <div className="work-tree-row" onClick={() => onToggleChapter(ch.id)}>
            <div className="work-tree-row-left">
              <i className={`pi ${expandedChapters.has(ch.id) ? 'pi-chevron-down' : 'pi-chevron-right'} text-sm`} />
              <i className="pi pi-book text-primary" />
              <div style={{ display: 'flex', flexDirection: 'column', gap: '2px' }}>
                <span className="font-bold">
                  {ch.titleIast || ch.titleEn}
                  {ch.titleDevanagari ? ` (${ch.titleDevanagari})` : ''}
                </span>
                <span className="text-xs text-color-secondary font-italic">
                  {i18n.language === 'ru' ? ch.titleRu : ch.titleEn}
                </span>
              </div>
            </div>
            {isAdmin && (
              <div className="work-tree-row-right">
                <IconButton
                  iconName="pi-plus"
                  className="p-button-sm"
                  tooltip={t('sangraha.addVerse')}
                  onClick={(e) => { e.stopPropagation(); onAddVerse(ch.id); }}
                />
                <IconButton
                  iconName="pi-trash"
                  className="p-button-sm p-button-danger"
                  tooltip={t('common.delete')}
                  onClick={(e) => { e.stopPropagation(); onDeleteChapter(ch.id); }}
                />
              </div>
            )}
          </div>
          {expandedChapters.has(ch.id) && ch.verses?.map((v: VerseTreeDto) => (
            <div
              key={v.id}
              className="work-tree-row work-tree-verse"
              onClick={() => navigate(`/sangraha/${workSlug}/verses/${v.id}`)}
            >
              <div className="work-tree-row-left" style={{ flexDirection: 'column', alignItems: 'flex-start', gap: '2px' }}>
                <div className="flex align-items-center gap-2">
                  <i className="pi pi-file text-color-secondary" />
                  <span className="text-sm">
                    {v.textIastPreview || `Verse ${v.orderIndex}`}
                    {v.textDevanagari ? ` (${v.textDevanagari})` : ''}
                  </span>
                  <Tag value={t(`sangraha.status.${v.status}`)} severity={statusSeverity[v.status] || 'info'} />
                </div>
                {translationFor(v) && (
                  <span className="text-xs text-color-secondary font-italic ml-4">
                    {translationFor(v)}
                  </span>
                )}
              </div>
              {isAdmin && (
                <div className="work-tree-row-right">
                  <IconButton
                    iconName="pi-trash"
                    className="p-button-sm p-button-danger"
                    tooltip={t('common.delete')}
                    onClick={(e) => { e.stopPropagation(); onDeleteVerse(v.id); }}
                  />
                </div>
              )}
            </div>
          ))}
        </div>
      ))}
    </div>
  );
}