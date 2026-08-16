import { useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { ProgressSpinner } from 'primereact/progressspinner';
import { Message } from 'primereact/message';
import { useSandhiRules } from '../hooks/useContent';
import { useLocation, useSearchParams } from 'react-router-dom';
const EmeneauRulesPage = () => {
  const { t } = useTranslation();
  const location = useLocation();
  const [searchParams, setSearchParams] = useSearchParams();
  const { data: response, isLoading, isError, error } = useSandhiRules();

  const allSandhiRules = response?.rules ?? [];
  const categoryGlossary = response?.categoryGlossary ?? {};
  const hasRuleParams = location.search.includes('rule=');

  const categories = useMemo(() => {
    const catSet = new Set<string>();
    allSandhiRules.forEach(r => r.category?.forEach(c => catSet.add(c)));
    return Array.from(catSet).sort();
  }, [allSandhiRules]);

  const selectedCategories = searchParams.getAll('category');

  const categoryOptions = useMemo(() => {
    return categories.map(cat => ({
      label: categoryGlossary[cat] ?? cat,
      value: cat,
    }));
  }, [categories, categoryGlossary]);

  const filteredRules = useMemo(() => {
    if (hasRuleParams) {
      const params = new URLSearchParams(location.search);
      const ruleParams = params.getAll('rule').map(Number).filter(n => !isNaN(n));

      if (ruleParams.length > 0) {
        const byNumber = new Map(allSandhiRules.map(r => [r.number, r]));
        const requested = new Set(ruleParams);
        const selected = new Set<number>();
        const stack = [...ruleParams];
        while (stack.length) {
          const n = stack.pop()!;
          if (selected.has(n)) continue;
          selected.add(n);
          byNumber.get(n)?.appliesWith?.forEach(d => stack.push(d));
        }

        const requestedRules = ruleParams
          .filter(n => byNumber.has(n))
          .map(n => byNumber.get(n)!);
        const dependencyRules = allSandhiRules
          .filter(r => selected.has(r.number) && !requested.has(r.number))
          .sort((a, b) => a.number - b.number);

        return { rules: [...requestedRules, ...dependencyRules], requested };
      }
    }

    let filtered = allSandhiRules;
    if (selectedCategories.length > 0) {
      filtered = allSandhiRules.filter(rule =>
        rule.category && selectedCategories.every(cat => rule.category!.includes(cat))
      );
    }

    return { rules: filtered, requested: null };
  }, [allSandhiRules, hasRuleParams, location.search, selectedCategories]);

  const { rules: sandhiRules, requested } = filteredRules;

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

  const renderRuleLinks = (numbers: number[]) =>
    numbers.map((d, i) => (
      <span key={d}>
        {i > 0 && ', '}
        <span
          className="cursor-pointer text-primary hover:underline"
          onClick={(e) => {
            e.stopPropagation();
            const el = document.getElementById(`rule-${d}`);
            if (el) {
              el.scrollIntoView({ behavior: 'smooth', block: 'center' });
            }
          }}
        >
          {d}
        </span>
      </span>
    ));

  return (
    <div className="flex flex-column align-items-center justify-content-center p-4">
      <h1 className="text-center mb-5">{t('grammar.sandhiRulesTitle')}</h1>

      {!hasRuleParams && categories.length > 0 && (
        <div className="w-full mb-3" style={{ maxWidth: '800px' }}>
          <div className="flex flex-wrap align-items-center gap-1">
            {categoryOptions.map(opt => {
              const isSelected = selectedCategories.includes(opt.value);
              return (
                <span
                  key={opt.value}
                  className={`inline-flex align-items-center px-2 py-1 cursor-pointer text-sm border-none ${
                    isSelected
                      ? 'bg-primary text-primary-contrast'
                      : 'bg-surface-100 text-color-secondary hover:bg-surface-200'
                  }`}
                  style={{ borderRadius: '4px', lineHeight: '1.2' }}
                  onClick={() => {
                    const newParams = new URLSearchParams(searchParams);
                    newParams.delete('category');
                    if (isSelected) {
                      selectedCategories.filter(c => c !== opt.value).forEach(c => newParams.append('category', c));
                    } else {
                      [...selectedCategories, opt.value].forEach(c => newParams.append('category', c));
                    }
                    setSearchParams(newParams, { replace: true });
                  }}
                >
                  {opt.label}
                </span>
              );
            })}
            {selectedCategories.length > 0 && (
              <span
                className="inline-flex align-items-center px-1 py-1 cursor-pointer text-sm text-color-secondary hover:text-color border-none"
                onClick={() => {
                  const newParams = new URLSearchParams(searchParams);
                  newParams.delete('category');
                  setSearchParams(newParams, { replace: true });
                }}
                title={t('eamenau.clearFilters')}
              >
                ✕
              </span>
            )}
          </div>
        </div>
      )}

      <div className="w-full" style={{ maxWidth: '800px' }}>
        {sandhiRules?.map((rule) => {
          const isDependency = requested !== null && !requested.has(rule.number);
          return (
            <div key={rule.number} id={`rule-${rule.number}`} className="mb-4">
              <div className="flex align-items-center gap-2 mb-2">
                <span
                  id={`rule-badge-${rule.number}`}
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
                {!isDependency && (
                  <span className="ml-auto flex flex-wrap gap-2 text-xs text-color-secondary">
                    {rule.supersedes && rule.supersedes.length > 0 && (
                      <span>Заменяет: № {renderRuleLinks(rule.supersedes)}</span>
                    )}
                    {rule.defaultFor && rule.defaultFor.length > 0 && (
                      <span>Общее для: № {renderRuleLinks(rule.defaultFor)}</span>
                    )}
                    {rule.appliesWith && rule.appliesWith.length > 0 && (
                      <span>Одновременно: № {renderRuleLinks(rule.appliesWith)}</span>
                    )}
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