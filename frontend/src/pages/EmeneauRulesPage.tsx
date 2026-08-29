import { useEffect, useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { ProgressSpinner } from 'primereact/progressspinner';
import { Message } from 'primereact/message';
import { useSandhiRules } from '../hooks/useContent';
import type { SandhiRuleDto } from '../types/content';
import { useLocation, useSearchParams } from 'react-router-dom';
const EmeneauRulesPage = () => {
  const { t } = useTranslation();
  const location = useLocation();
  const [searchParams, setSearchParams] = useSearchParams();
  const { data: response, isLoading, isError, error } = useSandhiRules();

  const [expanded, setExpanded] = useState<Set<number>>(new Set());
  const [scrollTarget, setScrollTarget] = useState<string | null>(null);

  useEffect(() => {
    if (scrollTarget !== null) {
      const el = document.getElementById(scrollTarget);
      if (el) {
        el.scrollIntoView({ behavior: 'smooth', block: 'center' });
        setScrollTarget(null);
      }
    }
  }, [scrollTarget, expanded]);

  const allSandhiRules = response?.rules ?? [];
  const categoryGlossary = response?.categoryGlossary ?? {};
  const hasRuleParams = location.search.includes('rule=');

  const categories = useMemo(() => {
    const catSet = new Set<string>();
    allSandhiRules.forEach(r => r.category?.forEach(c => catSet.add(c)));
    return Array.from(catSet).sort();
  }, [allSandhiRules]);

  const selectedCategories = searchParams.getAll('category');

  const [categoryMode, setCategoryMode] = useState<'AND' | 'OR'>(() => {
    const saved = localStorage.getItem('emeneau.categoryMode');
    return saved === 'OR' ? 'OR' : 'AND';
  });

  const changeCategoryMode = (mode: 'AND' | 'OR') => {
    setCategoryMode(mode);
    localStorage.setItem('emeneau.categoryMode', mode);
  };

  const categoryOptions = useMemo(() => {
    return categories.map(cat => ({
      label: categoryGlossary[cat] ?? cat,
      value: cat,
    }));
  }, [categories, categoryGlossary]);

  const linkedOf = (r?: SandhiRuleDto): number[] =>
    r ? [...(r.supersedes ?? []), ...(r.defaultFor ?? []), ...(r.appliesWith ?? [])] : [];

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
          linkedOf(byNumber.get(n)).forEach(d => stack.push(d));
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
        rule.category &&
        (categoryMode === 'AND'
          ? selectedCategories.every(cat => rule.category!.includes(cat))
          : selectedCategories.some(cat => rule.category!.includes(cat)))
      );
    }

    return { rules: filtered, requested: null };
  }, [allSandhiRules, hasRuleParams, location.search, selectedCategories]);

  const { rules: sandhiRules, requested } = filteredRules;

  const ownersMap = useMemo(() => {
    const byNumber = new Map(allSandhiRules.map(r => [r.number, r]));
    const map = new Map<number, Set<number>>();
    allSandhiRules.forEach(r => {
      linkedOf(r).forEach(d => {
        if (!byNumber.has(d)) return;
        if (!map.has(d)) map.set(d, new Set());
        map.get(d)!.add(r.number);
      });
    });
    return map;
  }, [allSandhiRules]);

  const isVisible = (rule: SandhiRuleDto): boolean => {
    if (requested === null) return true;
    if (requested.has(rule.number)) return true;
    const owners = ownersMap.get(rule.number);
    if (!owners || owners.size === 0) return false;
    return [...owners].some(o => expanded.has(o));
  };

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

  const renderRuleLinks = (numbers: number[], owner: number) =>
    numbers.map((d, i) => (
      <span key={d}>
        {i > 0 && ', '}
        <span
          className="cursor-pointer text-primary hover:underline"
          onClick={(e) => {
            e.stopPropagation();
            setExpanded(prev => {
              const next = new Set(prev);
              next.add(owner);
              return next;
            });
            setScrollTarget(`rule-${d}`);
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
                className="inline-flex align-items-center cursor-pointer text-sm text-color-secondary hover:text-color border-1 bg-surface-100 hover:bg-surface-200"
                style={{ padding: '2px 5px', borderRadius: '4px', lineHeight: '1.2', borderColor: 'var(--surface-300, #d4d4d8)' }}
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
            <span
              className="inline-flex align-items-center text-sm border-1"
              style={{ borderRadius: '4px', lineHeight: '1.2', borderColor: 'var(--surface-300, #d4d4d8)' }}
            >
              <span
                className={`cursor-pointer ${
                  categoryMode === 'AND'
                    ? 'bg-primary text-primary-contrast'
                    : 'bg-surface-100 text-color-secondary hover:bg-surface-200'
                }`}
                style={{ padding: '2px 5px', borderTopLeftRadius: '4px', borderBottomLeftRadius: '4px' }}
                onClick={() => changeCategoryMode('AND')}
              >
                {t('eamenau.filterAnd')}
              </span>
              <span
                className={`cursor-pointer ${
                  categoryMode === 'OR'
                    ? 'bg-primary text-primary-contrast'
                    : 'bg-surface-100 text-color-secondary hover:bg-surface-200'
                }`}
                style={{ padding: '2px 5px', borderTopRightRadius: '4px', borderBottomRightRadius: '4px' }}
                onClick={() => changeCategoryMode('OR')}
              >
                {t('eamenau.filterOr')}
              </span>
            </span>
          </div>
        </div>
      )}

      {hasRuleParams && categories.length > 0 && (
        <div className="w-full mb-3" style={{ maxWidth: '800px' }}>
          <span
            className="inline-flex align-items-center cursor-pointer text-sm border-1 bg-surface-100 hover:bg-surface-200"
            style={{ padding: '2px 5px', borderRadius: '4px', lineHeight: '1.2', borderColor: 'var(--surface-300, #d4d4d8)' }}
            onClick={() => {
              const newParams = new URLSearchParams(searchParams);
              newParams.delete('rule');
              setSearchParams(newParams, { replace: true });
            }}
          >
            {t('eamenau.allFilters')}
          </span>
        </div>
      )}

      <div className="w-full" style={{ maxWidth: '800px' }}>
        {sandhiRules?.map((rule) => {
          const isDependency = requested !== null && !requested.has(rule.number);
          if (isDependency && !isVisible(rule)) return null;
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
                {rule.applicability && (
                  <span className="text-xs text-color-secondary border-1 bg-surface-100"
                    style={{ padding: '1px 5px', borderRadius: '4px', lineHeight: '1.2', borderColor: 'var(--surface-300, #d4d4d8)' }}>
                    {categoryGlossary[rule.applicability] ?? rule.applicability}
                  </span>
                )}
                {rule.reference && (
                  <span className="text-sm text-color-secondary">({rule.reference})</span>
                )}
                {(rule.supersedes?.length || rule.defaultFor?.length || rule.appliesWith?.length) && (
                  <span className="ml-auto flex flex-wrap gap-2 text-xs text-color-secondary">
                    {rule.supersedes && rule.supersedes.length > 0 && (
                      <span>Заменяет: № {renderRuleLinks(rule.supersedes, rule.number)}</span>
                    )}
                    {rule.defaultFor && rule.defaultFor.length > 0 && (
                      <span>Общее для: № {renderRuleLinks(rule.defaultFor, rule.number)}</span>
                    )}
                    {rule.appliesWith && rule.appliesWith.length > 0 && (
                      <span>Одновременно: № {renderRuleLinks(rule.appliesWith, rule.number)}</span>
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