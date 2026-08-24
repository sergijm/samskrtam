import { useQuery } from '@tanstack/react-query';
import { dictionaryApi } from '../api/dictionaryApi';
import { MwEntryDto, FrischEntryDto, ApteEntryDto } from '../types';

export const useDictionarySearch = (query: string | null) => {
  return useQuery<MwEntryDto, Error>({
    queryKey: ['mw-entry', query],
    queryFn: () => dictionaryApi.searchByLemma(query!).then((res) => res.data),
    enabled: !!query,
    staleTime: 60 * 1000,
  });
};

export const useFrischLemma = (lemma: string | null) => {
  return useQuery<FrischEntryDto[], Error>({
    queryKey: ['frisch-lemma', lemma],
    queryFn: () => dictionaryApi.getFrischLemma(lemma!).then((res) => res.data),
    enabled: !!lemma,
    staleTime: 60 * 1000,
  });
};

export const useApteLemma = (lemma: string | null) => {
  return useQuery<ApteEntryDto[], Error>({
    queryKey: ['apte-lemma', lemma],
    queryFn: () => dictionaryApi.getApteLemma(lemma!).then((res) => res.data),
    enabled: !!lemma,
    staleTime: 60 * 1000,
  });
};

export const useMwEntry = (slp1Spelling: string | null) => {
  return useQuery<MwEntryDto, Error>({
    queryKey: ['mw-entry', slp1Spelling],
    queryFn: () => dictionaryApi.getMwEntry(slp1Spelling!).then((res) => res.data),
    enabled: !!slp1Spelling,
    staleTime: Infinity,
  });
};
