import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import type { ChapterSummaryDto } from '../../types/sangraha';

interface ChapterTreeBrowserProps {
  chapters: ChapterSummaryDto[];
  workSlug: string;
}

export default function ChapterTreeBrowser({
  chapters,
  workSlug,
}: ChapterTreeBrowserProps) {
  const { t, i18n } = useTranslation();
  const navigate = useNavigate();

  return (
    <div className="work-tree">
      {chapters?.map((ch: ChapterSummaryDto) => (
        <div
          key={ch.id}
          className="work-tree-row cursor-pointer hover:surface-hover"
          onClick={() => navigate(`/sangraha/${workSlug}/chapters/${ch.id}`)}
        >
          <div className="work-tree-row-left">
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
          <div className="work-tree-row-right">
            <span className="text-sm text-color-secondary">
              {ch.verseCount} {t('sangraha.verses')}
            </span>
            <i className="pi pi-chevron-right text-color-secondary ml-2" />
          </div>
        </div>
      ))}
    </div>
  );
}