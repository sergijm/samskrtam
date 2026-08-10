import { useParams } from 'react-router-dom';
import GrammarLessonPage from './GrammarLessonPage';
import SandhiLessonPage from './SandhiLessonPage';

const GrammarRouteResolver = () => {
  const { slug } = useParams<{ slug: string }>();

  if (slug?.startsWith('sandhi-')) {
    return <SandhiLessonPage />;
  }
  return <GrammarLessonPage />;
};

export default GrammarRouteResolver;