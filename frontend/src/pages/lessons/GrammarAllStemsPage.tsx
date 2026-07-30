import React, { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { Button } from 'primereact/button';
import { ToggleButton } from 'primereact/togglebutton';
import { Toast } from 'primereact/toast';
import { useStartOrResumeAllStemsSession } from '../../hooks/useQuiz';
import { useLocaleStore } from '../../store/localeStore';
import type { StartOrResumeResponse } from '../../types/quiz';

const VOWEL_TYPES: string[] = ['A_STEM', 'AA_STEM', 'I_STEM', 'II_STEM', 'U_STEM', 'UU_STEM', 'R_STEM',
    'PRON_AHAM', 'PRON_TVAM', 'PRON_TAD', 'PRON_ETAD', 'PRON_IDAM', 'PRON_KIM', 'PRON_YAD', 'PRON_REFLEXIVE'];
const NUMBER_TYPES: string[] = ['SINGULAR', 'DUAL', 'PLURAL'];
const GENDER_TYPES: string[] = ['MASCULINE', 'FEMININE', 'NEUTER', 'UNSPECIFIED'];
const CASE_TYPES: string[] = ['NOMINATIVE', 'ACCUSATIVE', 'INSTRUMENTAL', 'DATIVE', 'ABLATIVE', 'GENITIVE', 'LOCATIVE', 'VOCATIVE'];

const LS_KEY = 'allStemsFilters';

interface SavedFilters {
  vowels: string[];
  numbers: string[];
  genders: string[];
  cases: string[];
}

function loadFilters(): SavedFilters {
  try {
    const raw = localStorage.getItem(LS_KEY);
    if (raw) {
      const parsed = JSON.parse(raw);
      return {
        vowels: Array.isArray(parsed.vowels) ? parsed.vowels : [],
        numbers: Array.isArray(parsed.numbers) ? parsed.numbers : [],
        genders: Array.isArray(parsed.genders) ? parsed.genders : [],
        cases: Array.isArray(parsed.cases) ? parsed.cases : [],
      };
    }
  } catch {
    // corrupted, reset
  }
  return { vowels: [], numbers: [], genders: [], cases: [] };
}

function saveFilters(f: SavedFilters): void {
  localStorage.setItem(LS_KEY, JSON.stringify(f));
}

const GrammarAllStemsPage: React.FC = () => {
  const { t, i18n } = useTranslation();
  const navigate = useNavigate();
  const toast = React.useRef<Toast>(null);
  const { locale } = useLocaleStore();

  const [saved] = useState<SavedFilters>(loadFilters);

  const [selectedVowelTypes, setSelectedVowelTypes] = useState<string[]>(saved.vowels);
  const [selectedNumberTypes, setSelectedNumberTypes] = useState<string[]>(saved.numbers);
  const [selectedGenders, setSelectedGenders] = useState<string[]>(saved.genders);
  const [selectedCaseTypes, setSelectedCaseTypes] = useState<string[]>(saved.cases);

  const allStemsMutation = useStartOrResumeAllStemsSession();

  const isRu = i18n.language === 'ru';

  /* ---- generic toggle helper ---- */
  function toggleFilter<V extends string>(
    current: V[],
    setter: React.Dispatch<React.SetStateAction<V[]>>,
    value: V,
    field: keyof SavedFilters,
  ) {
    setter((prev) => {
      const next = prev.includes(value) ? prev.filter((v) => v !== value) : [...prev, value];
      saveFilters({
        vowels: field === 'vowels' ? next as string[] : selectedVowelTypes,
        numbers: field === 'numbers' ? next as string[] : selectedNumberTypes,
        genders: field === 'genders' ? next as string[] : selectedGenders,
        cases: field === 'cases' ? next as string[] : selectedCaseTypes,
      });
      return next;
    });
  }

  const handleStartQuiz = () => {
    allStemsMutation.mutate(
      {
        filterVowelTypes: selectedVowelTypes.length > 0 ? selectedVowelTypes : undefined,
        filterNumberTypes: selectedNumberTypes.length > 0 ? selectedNumberTypes : undefined,
        filterGenders: selectedGenders.length > 0 ? selectedGenders : undefined,
        filterCaseTypes: selectedCaseTypes.length > 0 ? selectedCaseTypes : undefined,
      },
      {
        onSuccess: (data: StartOrResumeResponse) => {
          navigate(`/quiz/grammar/declensions-all/${data.sessionId}`, {
            state: { sessionData: data },
          });
        },
        onError: (error: any) => {
          const errorCode = error?.response?.data?.code;
          if (errorCode === 'SCOPE_FILTER_EMPTY') {
            toast.current?.show({
              severity: 'warn',
              summary: '',
              detail: t('allStems.scopeFilterEmpty'),
              life: 5000,
            });
          } else {
            toast.current?.show({
              severity: 'error',
              summary: t('common.error'),
              detail: error?.message || t('quiz.startError', { message: '' }),
              life: 5000,
            });
          }
        },
      },
    );
  };

  const isLoading = allStemsMutation.isPending;

  return (
    <div className="p-4">
      <Toast ref={toast} />

      {/* Header */}
      <div className="card mb-4">
        <div className="flex align-items-center justify-content-between">
          <div>
            <h2 className="m-0">{t('allStems.title')}</h2>
            <p className="text-sm text-color-secondary m-0">
              {isRu ? 'Все основы' : 'All stems'}
            </p>
          </div>
        </div>
      </div>

      {/* Vowel Types */}
      <div className="field mb-3">
        <label className="block mb-2 font-medium">
          {t('allStems.filterVowelTypes')}
        </label>
        <div className="flex flex-wrap gap-2">
          {VOWEL_TYPES.map((vt) => (
            <ToggleButton
              key={vt}
              onLabel={t(`vowelType.${vt}`)}
              offLabel={t(`vowelType.${vt}`)}
              checked={selectedVowelTypes.includes(vt)}
              onChange={() => toggleFilter(selectedVowelTypes, setSelectedVowelTypes, vt, 'vowels')}
              className="p-button-sm"
            />
          ))}
        </div>
      </div>

      {/* Number Types */}
      <div className="field mb-3">
        <label className="block mb-2 font-medium">
          {t('allStems.filterNumberTypes')}
        </label>
        <div className="flex flex-wrap gap-2">
          {NUMBER_TYPES.map((nt) => (
            <ToggleButton
              key={nt}
              onLabel={t(`number.${nt}`)}
              offLabel={t(`number.${nt}`)}
              checked={selectedNumberTypes.includes(nt)}
              onChange={() => toggleFilter(selectedNumberTypes, setSelectedNumberTypes, nt, 'numbers')}
              className="p-button-sm"
            />
          ))}
        </div>
      </div>

      {/* Gender */}
      <div className="field mb-3">
        <label className="block mb-2 font-medium">
          {t('allStems.filterGenders')}
        </label>
        <div className="flex flex-wrap gap-2">
          {GENDER_TYPES.map((g) => (
            <ToggleButton
              key={g}
              onLabel={t(`gender.${g}`)}
              offLabel={t(`gender.${g}`)}
              checked={selectedGenders.includes(g)}
              onChange={() => toggleFilter(selectedGenders, setSelectedGenders, g, 'genders')}
              className="p-button-sm"
            />
          ))}
        </div>
      </div>

      {/* Case Types */}
      <div className="field mb-0">
        <label className="block mb-2 font-medium">
          {t('allStems.filterCaseTypes')}
        </label>
        <div className="flex flex-wrap gap-2">
          {CASE_TYPES.map((ct) => (
            <ToggleButton
              key={ct}
              onLabel={t(`case.${ct}`)}
              offLabel={t(`case.${ct}`)}
              checked={selectedCaseTypes.includes(ct)}
              onChange={() => toggleFilter(selectedCaseTypes, setSelectedCaseTypes, ct, 'cases')}
              className="p-button-sm"
            />
          ))}
        </div>
      </div>

      {/* Start Quiz Button */}
      <div className="flex justify-content-center mt-4">
        <Button
          label={t('allStems.startQuiz')}
          icon="pi pi-play"
          size="large"
          loading={isLoading}
          onClick={handleStartQuiz}
          className="px-6"
        />
      </div>
    </div>
  );
};

export default GrammarAllStemsPage;
