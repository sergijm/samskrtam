import { useTranslation } from 'react-i18next';
import { ProgressSpinner } from 'primereact/progressspinner';
import { Message } from 'primereact/message';
import { useSandhiRules } from '../hooks/useContent';
import { useLocation } from 'react-router-dom';

const EmeneauRulesPage = () => {
  const { t, i18n } = useTranslation();
  const location = useLocation();
  const { data: allSandhiRules, isLoading, isError, error } = useSandhiRules();

  const isRu = i18n.language === 'ru';
  const dependsOnLabel = isRu ? 'Зависит от' : 'Depends on';

  const getFilteredRules = () => {
    const params = new URLSearchParams(location.search);
    const ruleParams = params.getAll('rule').map(Number).filter(n => !isNaN(n));

    if (ruleParams.length > 0 && allSandhiRules) {
      const byNumber = new Map(allSandhiRules.map(r => [r.number, r]));
      const requested = new Set(ruleParams);
      const selected = new Set<number>();
      const stack = [...ruleParams];
      while (stack.length) {
        const n = stack.pop()!;
        if (selected.has(n)) continue;
        selected.add(n);
        byNumber.get(n)?.dependsOn?.forEach(d => stack.push(d));
      }

      const requestedRules = ruleParams
        .filter(n => byNumber.has(n))
        .map(n => byNumber.get(n)!);
      const dependencyRules = allSandhiRules
        .filter(r => selected.has(r.number) && !requested.has(r.number))
        .sort((a, b) => a.number - b.number);

      return { rules: [...requestedRules, ...dependencyRules], requested };
    }
    return { rules: allSandhiRules, requested: null };
  };

  const { rules: sandhiRules, requested } = getFilteredRules();

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
        <Message severity="error" text={t('emeneau.fetchRulesError', { message: error?.message })} />
      </div>
    );
  }

  return (
    <div className="flex flex-column align-items-center justify-content-center p-4">
      <h1 className="text-center mb-5">{t('grammar.sandhiRulesTitle')}</h1>
      <div className="w-full" style={{ maxWidth: '800px' }}>
        {sandhiRules?.map((rule) => {
          const isDependency = requested !== null && !requested.has(rule.number);
          return (
            <div key={rule.number} className="mb-4">
              <div className="flex align-items-center gap-2 mb-2">
                <span
                  className={
                    isDependency
                      ? 'bg-surface-300 text-color border-circle w-2rem h-2rem flex align-items-center justify-content-center font-bold text-sm'
                      : 'bg-orange-500 text-white border-circle w-2rem h-2rem flex align-items-center justify-content-center font-bold text-sm'
                  }
                >
                  {rule.number}
                </span>
                {rule.reference && (
                  <span className="text-sm text-color-secondary">({rule.reference})</span>
                )}
                {!isDependency && rule.dependsOn && rule.dependsOn.length > 0 && (
                  <span className="ml-auto text-xs text-color-secondary">
                    {dependsOnLabel}: № {rule.dependsOn.join(', ')}
                  </span>
                )}
              </div>
              <p className="m-0 mb-1">{rule.text}</p>
              {rule.example && (
                <p className="m-0 text-color-secondary font-italic">{rule.example}</p>
              )}
            </div>
          );
        })}
        {(!sandhiRules || sandhiRules.length === 0) && (
          <Message severity="info" text={t('emeneau.noRulesFound')} />
        )}
      </div>
    </div>
  );
};

export default EmeneauRulesPage;