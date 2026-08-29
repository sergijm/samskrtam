import React, { useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { useDeclensionLessons } from '../../hooks/useQuiz';
import { ProgressBar } from 'primereact/progressbar';
import { Message } from 'primereact/message';
import { ProgressSpinner } from 'primereact/progressspinner';
import { LessonItemDto } from '../../types/quiz';
import ProgressSummaryPanel from '../../components/dashboard/ProgressSummaryPanel';

interface DomainMeta {
  labelRu: string;
  labelEn: string;
  icon: string;
}

const DOMAIN_META: Record<string, DomainMeta> = {
  PHONOLOGY_SCRIPT:   { labelRu: 'Письменность и фонология',       labelEn: 'Script & Phonology',       icon: 'pi-pencil' },
  SANDHI:             { labelRu: 'Sandhi',                         labelEn: 'Sandhi',                   icon: 'pi-sync' },
  GRAMMAR_FOUNDATIONS:{ labelRu: 'Основы грамматики',             labelEn: 'Grammar Foundations',      icon: 'pi-info-circle' },
  NOMINAL_MORPHOLOGY: { labelRu: 'Склонение',                     labelEn: 'Nominal Morphology',       icon: 'pi-table' },
  PRONOUNS:           { labelRu: 'Местоимения',                   labelEn: 'Pronouns',                 icon: 'pi-user' },
  VERBAL_MORPHOLOGY:  { labelRu: 'Глагольная морфология',        labelEn: 'Verbal Morphology',        icon: 'pi-play' },
  NONFINITE_FORMS:    { labelRu: 'Неличные формы глагола',        labelEn: 'Non-finite Forms',         icon: 'pi-list' },
  NUMERALS:           { labelRu: 'Числительные',                  labelEn: 'Numerals',                 icon: 'pi-sort-numeric-down' },
  CASE_SYNTAX:        { labelRu: 'Падежи и kāraka',              labelEn: 'Case & kāraka',            icon: 'pi-tag' },
  SYNTAX:             { labelRu: 'Синтаксис',                     labelEn: 'Syntax',                   icon: 'pi-sitemap' },
  WORD_FORMATION:     { labelRu: 'Словообразование',             labelEn: 'Word Formation',           icon: 'pi-prime' },
  ADVANCED_READING:   { labelRu: 'Продвинутое чтение',           labelEn: 'Advanced Reading',         icon: 'pi-book' },
  GRAMMAR:            { labelRu: 'Грамматика',                    labelEn: 'Grammar',                  icon: 'pi-book' },
};

const GrammarPage = () => {
  const { t, i18n } = useTranslation();
  const navigate = useNavigate();
  const isRu = i18n.language === 'ru';

  const { data: lessons = [], isLoading, isError } = useDeclensionLessons();

  const grouped = useMemo(() => {
    const map = new Map<string, LessonItemDto[]>();
    for (const l of lessons) {
      const d = l.domain || 'GRAMMAR';
      if (!map.has(d)) map.set(d, []);
      map.get(d)!.push(l);
    }
    return Array.from(map.entries())
      .sort(([a], [b]) => a.localeCompare(b));
  }, [lessons]);

  if (isLoading) {
    return (
      <div className="flex justify-content-center align-items-center min-h-screen">
        <ProgressSpinner />
      </div>
    );
  }

  if (isError) {
    return (
      <div className="flex justify-content-center align-items-center min-h-screen">
        <Message severity="error" text={t('quizzes.fetchError')} />
      </div>
    );
  }

  return (
    <div className="grammar-page p-3 md:p-4">
      <div className="lexicon-hero mb-5">
        <div className="flex flex-column md:flex-row md:align-items-end md:justify-content-between gap-3">
          <div>
            <h1 className="m-0 text-3xl">{t('nav.grammar')}</h1>
            <p className="m-0 text-500 mt-1">{t('grammar.subtitle')}</p>
          </div>
          <div className="w-full md:w-20rem">
            <ProgressSummaryPanel scope="grammar" showScopeTitle={false} />
          </div>
        </div>
        <div className="lexicon-hero-stats mt-4">
          <div className="flex justify-content-between align-items-center mb-1">
            <span className="font-semibold">
              {lessons.length} {isRu ? 'тем' : 'topics'} · {grouped.length} {isRu ? 'групп' : 'groups'}
            </span>
          </div>
          <ProgressBar value={100} style={{ height: '0.75rem' }} />
        </div>
      </div>

      {grouped.map(([domain, items]) => {
        const meta = DOMAIN_META[domain] || { labelRu: domain, labelEn: domain, icon: 'pi-folder' };
        const label = isRu ? meta.labelRu : meta.labelEn;
        return (
          <section key={domain} className="mb-5">
            <div className="flex align-items-center gap-2 mb-3">
              <i className={`pi ${meta.icon} text-lg text-primary`} />
              <h2 className="m-0 text-xl">{label}</h2>
              <span className="text-sm text-500">({items.length})</span>
            </div>
            <div className="grid">
              {items.map((lesson) => (
                <GrammarCard key={lesson.id} lesson={lesson} onClick={() => navigate(`/lessons/grammar/${lesson.slug}`)} />
              ))}
            </div>
          </section>
        );
      })}

      {lessons.length === 0 && (
        <Message severity="info" text={t('grammar.noLessonsFound')} />
      )}
    </div>
  );
};

const GrammarCard = ({ lesson, onClick }: { lesson: LessonItemDto; onClick: () => void }) => {
  const { i18n } = useTranslation();
  const isRu = i18n.language === 'ru';
  const title = isRu ? lesson.titleRu : lesson.titleEn;

  return (
    <div className="col-12 sm:col-6 lg:col-4 xl:col-3">
      <div className="grammar-card h-full cursor-pointer" onClick={onClick}>
        <div className="flex align-items-center justify-content-between gap-2">
          <span className="font-semibold">{title}</span>
          <span className="text-sm text-500">
            {lesson.totalQuestions} {isRu ? 'вопр.' : 'q.'}
          </span>
        </div>
        {lesson.difficulty && (
          <div className="text-sm text-500 mt-1">{lesson.difficulty}</div>
        )}
        <ProgressBar value={0} style={{ height: '0.4rem' }} className="mt-2" />
      </div>
    </div>
  );
};

export default GrammarPage;