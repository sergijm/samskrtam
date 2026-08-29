import { useParams } from 'react-router-dom';
import GrammarLessonPage from './GrammarLessonPage';
import SandhiLessonPage from './SandhiLessonPage';
import CaseMeaningsLessonPage from './CaseMeaningsLessonPage';
import ConjugationLessonPage from './ConjugationLessonPage';
import CaseEndingsLessonPage from './CaseEndingsLessonPage';
import VerbalEndingsLessonPage from './VerbalEndingsLessonPage';

const CONJUGATION_SLUGS: string[] = ['presence-indicativus', 'imperfectum', 'optativus', 'imperativus'];

const GrammarRouteResolver = () => {
  const { slug } = useParams<{ slug: string }>();

  if (slug?.startsWith('sandhi-')) {
    return <SandhiLessonPage />;
  }
  if (slug === 'case-meanings-basic') {
    return <CaseMeaningsLessonPage />;
  }
  if (slug === 'case-endings') {
    return <CaseEndingsLessonPage />;
  }
  if (slug === 'verbal-endings') {
    return <VerbalEndingsLessonPage />;
  }
  if (slug && CONJUGATION_SLUGS.includes(slug)) {
    return <ConjugationLessonPage />;
  }
  return <GrammarLessonPage />;
};

export default GrammarRouteResolver;