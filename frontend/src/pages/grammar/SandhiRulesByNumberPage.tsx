import { useSearchParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useSandhiRulesByNumbers } from '../../hooks/useLessons';
import { Skeleton } from 'primereact/skeleton';

const SandhiRulesByNumberPage = () => {
  const [searchParams] = useSearchParams();
  const { t, i18n } = useTranslation();

  const ruleNumbers = searchParams.getAll('rule').map(Number).filter(n => !isNaN(n));
  const { data: response, isLoading, isError } = useSandhiRulesByNumbers(ruleNumbers);

  const isRu = i18n.language === 'ru';
  const requestedSet = new Set(ruleNumbers);

  const dependencyLabel = isRu ? 'зависимость' : 'dependency';
  const dependsOnLabel = isRu ? 'Зависит от' : 'Depends on';

  return (
    <div className="p-4">
      <div className="card mb-3">
        <h2 className="m-0">
          {isRu ? 'Правила сандхи' : 'Sandhi rules'}
          {ruleNumbers.length > 0 && (
            <span className="text-color-secondary text-base ml-2">
              № {ruleNumbers.join(', ')}
            </span>
          )}
        </h2>
      </div>

      {isLoading || !response ? (
        <div className="p-4">
          <Skeleton width="100%" height="40px" className="mb-2" />
          <Skeleton width="100%" height="20px" className="mb-2" />
          <Skeleton width="100%" height="200px" />
        </div>
      ) : isError ? (
        <div className="p-error">{t('lesson.loadError')}</div>
      ) : response.rules.length === 0 ? (
        <div className="card">
          <p className="text-color-secondary m-0">
            {isRu ? 'Правила не найдены.' : 'No rules found.'}
          </p>
        </div>
      ) : (
        <div className="flex flex-column gap-3">
          {[...response.rules.filter(r => requestedSet.has(r.number)),
            ...response.rules.filter(r => !requestedSet.has(r.number))].map((rule) => {
            const isDependency = !requestedSet.has(rule.number);
            return (
              <div key={rule.number} className="card p-0">
                <div className="surface-ground p-3 border-bottom-1 border-200">
                  <div className="flex align-items-center gap-2">
                    <span
                      className={
                        isDependency
                          ? 'bg-surface-300 text-color border-circle w-2rem h-2rem flex align-items-center justify-content-center font-bold'
                          : 'bg-primary text-white border-circle w-2rem h-2rem flex align-items-center justify-content-center font-bold'
                      }
                    >
                      {rule.number}
                    </span>
                    {isDependency && (
                      <span className="text-xs uppercase text-color-secondary">
                        {dependencyLabel}
                      </span>
                    )}
                    <span className="text-sm text-color-secondary">
                      {rule.reference}
                    </span>
                    {rule.dependsOn && rule.dependsOn.length > 0 && (
                      <span className="ml-auto text-xs text-color-secondary">
                        {dependsOnLabel}: № {rule.dependsOn.join(', ')}
                      </span>
                    )}
                  </div>
                </div>
                <div className="p-3">
                  <p className="m-0 mb-2 text-lg">{rule.text}</p>
                  {rule.example && (
                    <div className="surface-50 p-3 border-round text-base">
                      <span className="font-mono">{rule.example}</span>
                    </div>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};

export default SandhiRulesByNumberPage;