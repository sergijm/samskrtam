import { useParams } from 'react-router-dom';
import GrammarLessonPage from './GrammarLessonPage';
import SandhiLessonPage from './SandhiLessonPage';
import CaseMeaningsLessonPage from './CaseMeaningsLessonPage';

const GrammarRouteResolver = () => {
  const { slug } = useParams<{ slug: string }>();

  if (slug?.startsWith('sandhi-')) {
    return <SandhiLessonPage />;
  }
  if (slug === 'case-meanings-basic') {
    return <CaseMeaningsLessonPage />;
  }
  return <GrammarLessonPage />;
};

export default GrammarRouteResolver;