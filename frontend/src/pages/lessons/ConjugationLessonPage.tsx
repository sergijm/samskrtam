import { useState } from 'react';
import { useParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { Skeleton } from 'primereact/skeleton';
import { TabView, TabPanel } from 'primereact/tabview';
import { useGrammarLesson } from '../../hooks/useLessons';
import { LessonHeader } from '../../components/lesson/LessonHeader';
import { LessonStatsTab } from '../../components/lesson/LessonStatsTab';
import ConjugationEndingsTable from '../../components/lesson/ConjugationEndingsTable';
import ConjugationParadigmCarousel from '../../components/lesson/ConjugationParadigmCarousel';
import ConjugationExamplesPanel from '../../components/lesson/ConjugationExamplesPanel';
import ConjugationProgressGrid from '../../components/lesson/ConjugationProgressGrid';
import { PRESENT_ENDINGS } from '../../data/presentConjugation';
import { IMPERFECT_ENDINGS } from '../../data/imperfectConjugation';
import { OPTATIVE_ENDINGS } from '../../data/optativeConjugation';
import { IMPERATIVE_ENDINGS } from '../../data/imperativeConjugation';
import type { Voice } from '../../types/content-dtos';

type SelectedVoice = Voice | null;

const voiceKey = (v: Voice) => (v === 'PARASMAIPADA' ? 'parasmaipada' : 'atmanepada');

const SLUG_TO_TENSE_MOOD: Record<string, { tense: string; mood: string }> = {
  'presence-indicativus': { tense: 'PRESENT', mood: 'INDICATIVE' },
  'imperfectum':          { tense: 'IMPERFECT', mood: 'INDICATIVE' },
  'optativus':            { tense: 'PRESENT', mood: 'OPTATIVE' },
  'imperativus':          { tense: 'PRESENT', mood: 'IMPERATIVE' },
};

const ConjugationLessonPage = () => {
  const { slug } = useParams<{ slug: string }>();
  const isImperfect = slug === 'imperfectum';
  const isOptative = slug === 'optativus';
  const isImperative = slug === 'imperativus';
  const endings = isOptative ? OPTATIVE_ENDINGS : isImperfect ? IMPERFECT_ENDINGS : isImperative ? IMPERATIVE_ENDINGS : PRESENT_ENDINGS;
  const endingsTitleKey = isOptative ? 'optativeEndingsTitle' : isImperfect ? 'imperfectEndingsTitle' : isImperative ? 'imperativeEndingsTitle' : 'presentEndingsTitle';
  const { t, i18n } = useTranslation();
  const { data: lesson, isLoading } = useGrammarLesson(slug || '');

  const [selectedVoice, setSelectedVoice] = useState<SelectedVoice>(null);

  const voices: Voice[] = ['PARASMAIPADA', 'ATMANEPADA'];

  return (
    <div className="p-4">
      {isLoading || !lesson ? (
        <div className="p-4">
          <Skeleton width="100%" height="40px" className="mb-2" />
          <Skeleton width="100%" height="20px" className="mb-2" />
          <Skeleton width="100%" height="200px" />
        </div>
      ) : (
        <>
          <div className="card mb-3">
            <LessonHeader title={lesson.titleRu} titleEn={lesson.titleEn} />
          </div>

          {lesson.statusSummary && (
            <div className="mb-3">
              <LessonStatsTab
                statusSummary={lesson.statusSummary}
                quizPath={`/quiz/grammar/${slug}`}
              />
            </div>
          )}

          <div className="card mb-4">
            <div className="text-lg font-semibold mb-2">
              {t(`grammar.${endingsTitleKey}`)}
            </div>
            <div className="grid">
              {voices.map((v) => {
                const active = selectedVoice === v;
                const headerClass = active
                  ? 'bg-primary text-white hover:bg-primary'
                  : 'bg-gray-50 text-color hover:bg-gray-100 cursor-pointer';
                return (
                  <div key={v} className="col-12 md:col-6 flex flex-column align-items-center">
                    <div
                      className={`p-2 font-semibold text-center border-1 border-200 border-round ${headerClass}`}
                      style={{ cursor: 'pointer', userSelect: 'none', width: 'fit-content' }}
                      onClick={() => setSelectedVoice(active ? null : v)}
                      role="button"
                      tabIndex={0}
                      onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); setSelectedVoice(active ? null : v); } }}
                    >
                      {v === 'PARASMAIPADA' ? 'Parasmaipada' : 'Ātmanepada'}
                    </div>
                    <ConjugationEndingsTable voice={voiceKey(v)} endings={endings} />
                  </div>
                );
              })}
            </div>
          </div>

          <TabView>
            <TabPanel header={i18n.language === 'ru' ? 'Парадигмы' : 'Paradigms'}>
              <div className="card p-4 md:p-5">
                <div className="text-lg font-semibold mb-3">
                  {t('grammar.paradigmSectionTitle')}
                </div>
                <ConjugationParadigmCarousel slug={slug || ''} voice={selectedVoice} enabled={true} />
              </div>
            </TabPanel>
            <TabPanel header={i18n.language === 'ru' ? 'Примеры' : 'Examples'}>
              <div className="card p-4 md:p-5">
                <ConjugationExamplesPanel
                  slug={slug || ''}
                  tense={SLUG_TO_TENSE_MOOD[slug ?? '']?.tense ?? null}
                  mood={SLUG_TO_TENSE_MOOD[slug ?? '']?.mood ?? null}
                  enabled={true}
                />
              </div>
            </TabPanel>
            <TabPanel header={i18n.language === 'ru' ? 'Прогресс' : 'Progress'}>
              <div className="card p-4 md:p-5">
                <ConjugationProgressGrid
                  progress={lesson.conjugationProgress ?? []}
                  quizSlug={slug || ''}
                />
              </div>
            </TabPanel>
          </TabView>
        </>
      )}
    </div>
  );
};

export default ConjugationLessonPage;