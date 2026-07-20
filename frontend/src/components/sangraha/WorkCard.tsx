import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { IconButton } from '../common/buttons';

interface WorkCardProps {
  id: string;
  slug: string;
  titleEn: string;
  titleRu: string;
  author?: string;
  descriptionEn?: string;
  isAdmin: boolean;
  onDelete: (slug: string) => void;
}

export default function WorkCard({ slug, titleEn, titleRu, author, descriptionEn, isAdmin, onDelete }: WorkCardProps) {
  const { t } = useTranslation();
  const navigate = useNavigate();

  return (
    <div className="col-12 sm:col-6 md:col-4 lg:col-3 p-2 flex">
      <div
        onClick={() => navigate(`/sangraha/${slug}`)}
        className="p-card p-component lesson-card flex flex-column text-center h-full cursor-pointer hover:shadow-8 transition-all transition-duration-200 w-full"
      >
        <div className="p-card-body">
          <div className="p-card-title">{titleEn}</div>
          <div className="p-card-subtitle">{titleRu}</div>
          {author && <div className="mt-2 text-sm text-color-secondary">{author}</div>}
          {descriptionEn && (
            <div className="mt-2 text-sm text-color-secondary">{descriptionEn}</div>
          )}
        </div>
        {isAdmin && (
          <div className="p-card-footer flex justify-content-end">
            <IconButton
              iconName="pi-trash"
              className="p-button-danger"
              onClick={(e) => {
                e.stopPropagation();
                onDelete(slug);
              }}
            />
          </div>
        )}
      </div>
    </div>
  );
}