import { useTranslation } from 'react-i18next';
import { Dropdown } from 'primereact/dropdown';
import { Button } from 'primereact/button';
import { LessonType, SessionStatus } from '../../types/quiz';

interface QuizSessionFiltersProps {
  quizTypeFilter: LessonType | undefined;
  statusFilter: SessionStatus | undefined;
  onQuizTypeChange: (value: LessonType | undefined) => void;
  onStatusChange: (value: SessionStatus | undefined) => void;
  onReset: () => void;
}

const quizTypeOptions = [
  { label: 'common.all', value: undefined },
  { label: 'lessonType.VOCABULARY', value: 'VOCABULARY' as LessonType },
  { label: 'lessonType.DECLENSIONS', value: 'DECLENSIONS' as LessonType },
  { label: 'lessonType.CONJUGATIONS', value: 'CONJUGATIONS' as LessonType },
  { label: 'lessonType.A_STEM_DECLENSIONS', value: 'A_STEM_DECLENSIONS' as LessonType },
  { label: 'lessonType.AA_STEM_DECLENSIONS', value: 'AA_STEM_DECLENSIONS' as LessonType },
  { label: 'lessonType.I_STEM_DECLENSIONS', value: 'I_STEM_DECLENSIONS' as LessonType },
  { label: 'lessonType.II_STEM_DECLENSIONS', value: 'II_STEM_DECLENSIONS' as LessonType },
  { label: 'lessonType.U_STEM_DECLENSIONS', value: 'U_STEM_DECLENSIONS' as LessonType },
  { label: 'lessonType.UU_STEM_DECLENSIONS', value: 'UU_STEM_DECLENSIONS' as LessonType },
  { label: 'lessonType.R_STEM_DECLENSIONS', value: 'R_STEM_DECLENSIONS' as LessonType },
];

const sessionStatusOptions = [
  { label: 'common.all', value: undefined },
  { label: 'sessionStatus.IN_PROGRESS', value: 'IN_PROGRESS' as SessionStatus },
  { label: 'sessionStatus.COMPLETED', value: 'COMPLETED' as SessionStatus },
  { label: 'sessionStatus.ABANDONED', value: 'ABANDONED' as SessionStatus },
];

export default function QuizSessionFilters({
  quizTypeFilter,
  statusFilter,
  onQuizTypeChange,
  onStatusChange,
  onReset,
}: QuizSessionFiltersProps) {
  const { t } = useTranslation();

  return (
    <div className="flex flex-wrap gap-3 mb-4">
      <Dropdown
        value={quizTypeFilter}
        options={quizTypeOptions.map((opt) => ({ ...opt, label: t(opt.label) }))}
        onChange={(e) => onQuizTypeChange(e.value)}
        placeholder={t('common.filterByQuizType')}
        className="w-12rem"
      />
      <Dropdown
        value={statusFilter}
        options={sessionStatusOptions.map((opt) => ({ ...opt, label: t(opt.label) }))}
        onChange={(e) => onStatusChange(e.value)}
        placeholder={t('common.filterByStatus')}
        className="w-12rem"
      />
      <Button icon="pi pi-filter-slash" className="p-button-outlined" onClick={onReset} />
    </div>
  );
}