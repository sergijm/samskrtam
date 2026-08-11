import { create } from 'zustand';
import type { FilterScope } from '../api/quizApi';

interface QuizFilterState {
  filterScope: FilterScope | null;
  filterCaseType: string | null;
  filterNumberType: string | null;
  filterGender: string | null;

  setFilter: (
    filterScope: FilterScope,
    filterCaseType: string,
    filterNumberType?: string | null,
    filterGender?: string | null
  ) => void;
  clearFilter: () => void;
}

export const useQuizFilterStore = create<QuizFilterState>()(
  (set) => ({
    filterScope: null,
    filterCaseType: null,
    filterNumberType: null,
    filterGender: null,

    setFilter: (filterScope, filterCaseType, filterNumberType = null, filterGender = null) =>
      set({
        filterScope,
        filterCaseType,
        filterNumberType: filterScope === 'CASE_NUMBER_GENDER' ? filterNumberType : null,
        filterGender: filterScope === 'CASE_NUMBER_GENDER' ? filterGender : null,
      }),

    clearFilter: () =>
      set({
        filterScope: null,
        filterCaseType: null,
        filterNumberType: null,
        filterGender: null,
      }),
  })
);