import { useQuery } from '@tanstack/react-query';
import { dictionaryApi } from '../api/dictionaryApi';
import { MwWordSearchDto, MwEntryDto } from '../types';

export const useMwWordSearch = (query: string | null) => {
  return useQuery<MwWordSearchDto[], Error>({
    queryKey: ['mw-word-search', query],
    queryFn: () => dictionaryApi.searchMwWords(query!).then((res) => res.data),
    enabled: !!query, // Запрос будет выполняться только если query не пустой
    staleTime: 60 * 1000, // Кэшируем результаты на 1 минуту
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
