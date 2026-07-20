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
  const { t } = useTranslation();
  const navigate = useNavigate();

  return (
    <div className="work-tree">
      {chapters?.map((ch: ChapterTreeDto) => (
        <div key={ch.id} className="work-tree-chapter">
          <div className="work-tree-row" onClick={() => onToggleChapter(ch.id)}>
            <div className="work-tree-row-left">
              <i className={`pi ${expandedChapters.has(ch.id) ? 'pi-chevron-down' : 'pi-chevron-right'} text-sm`} />
              <i className="pi pi-book text-primary" />
              <span className="font-bold">{ch.titleEn} ({ch.titleRu})</span>
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
              <div className="work-tree-row-left">
                <i className="pi pi-file text-color-secondary" />
                <span className="text-sm">{v.textIastPreview || `Verse ${v.orderIndex}`}</span>
                <Tag value={t(`sangraha.status.${v.status}`)} severity={statusSeverity[v.status] || 'info'} />
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