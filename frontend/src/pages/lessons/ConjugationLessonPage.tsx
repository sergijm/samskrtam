import { useState } from 'react';
import { useParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { Skeleton } from 'primereact/skeleton';
import { TabView, TabPanel } from 'primereact/tabview';
import { useGrammarLesson } from '../../hooks/useLessons';
import { LessonHeader } from '../../components/lesson/LessonHeader';
import ConjugationEndingsTable from '../../components/lesson/ConjugationEndingsTable';
import ConjugationParadigmCarousel from '../../components/lesson/ConjugationParadigmCarousel';
import { PRESENT_ENDINGS } from '../../data/presentConjugation';
import { IMPERFECT_ENDINGS } from '../../data/imperfectConjugation';
import { OPTATIVE_ENDINGS } from '../../data/optativeConjugation';
import { IMPERATIVE_ENDINGS } from '../../data/imperativeConjugation';
import type { Voice } from '../../types/content-dtos';

const CONJUGATION_VOICE_STORAGE_KEY = 'conjugation-lesson-active-voice';

function readSavedVoice(): Voice {
  try {
    const raw = localStorage.getItem(CONJUGATION_VOICE_STORAGE_KEY);
    if (raw === 'ATMANEPADA' || raw === 'PARASMAIPADA') return raw;
  } catch { /* ignore */ }
  return 'PARASMAIPADA';
}

const voiceKey = (v: Voice) => (v === 'PARASMAIPADA' ? 'parasmaipada' : 'atmanepada');

const voiceTabLabel = (v: Voice) => (v === 'PARASMAIPADA' ? 'Parasmaipada' : 'Ātmanepada');

const indexOfVoice = (v: Voice): number => (v === 'PARASMAIPADA' ? 0 : 1);
const voiceOfIndex = (i: number): Voice => (i === 0 ? 'PARASMAIPADA' : 'ATMANEPADA');

const ConjugationLessonPage = () => {
  const { slug } = useParams<{ slug: string }>();
  const isImperfect = slug === 'imperfectum';
  const isOptative = slug === 'optativus';
  const isImperative = slug === 'imperativus';
  const endings = isOptative ? OPTATIVE_ENDINGS : isImperfect ? IMPERFECT_ENDINGS : isImperative ? IMPERATIVE_ENDINGS : PRESENT_ENDINGS;
  const endingsTitleKey = isOptative ? 'optativeEndingsTitle' : isImperfect ? 'imperfectEndingsTitle' : isImperative ? 'imperativeEndingsTitle' : 'presentEndingsTitle';
  const { t } = useTranslation();
  const { data: lesson, isLoading } = useGrammarLesson(slug || '');

  const [activeTab, setActiveTab] = useState<number>(() => indexOfVoice(readSavedVoice()));

  const onTabChange = (index: number) => {
    setActiveTab(index);
    try {
      localStorage.setItem(CONJUGATION_VOICE_STORAGE_KEY, voiceOfIndex(index));
    } catch { /* ignore */ }
  };

  const renderVoiceContent = (v: Voice) => (
    <>
      <div className="card mb-4">
        <div className="text-lg font-semibold mb-2">
          {t(`grammar.${endingsTitleKey}`)}
        </div>
        <ConjugationEndingsTable voice={voiceKey(v)} endings={endings} />
      </div>

      <div className="card p-4 md:p-5">
        <div className="text-lg font-semibold mb-3">
          {t('grammar.paradigmSectionTitle')}
        </div>
        <ConjugationParadigmCarousel slug={slug || ''} voice={v} enabled={true} />
      </div>
    </>
  );

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

          <TabView
            activeIndex={activeTab}
            onTabChange={(e) => onTabChange(e.index)}
            renderActiveOnly={true}
            className="mb-3"
          >
            {(['PARASMAIPADA', 'ATMANEPADA'] as Voice[]).map((v, i) => (
              <TabPanel key={i} header={voiceTabLabel(v)}>
                {renderVoiceContent(v)}
              </TabPanel>
            ))}
          </TabView>
        </>
      )}
    </div>
  );
};

export default ConjugationLessonPage;