import api from './axios';
import { MwEntryDto, FrischEntryDto, ApteEntryDto, LemmaSearchResultDto, DictionaryEntriesResponseDto } from '../types';

export const dictionaryApi = {
  searchByLemma: (query: string) => {
    return api.get<MwEntryDto>(`/api/v1/dictionary/search?query=${query}`);
  },
  getMwEntry: (slp1Spelling: string) => {
    return api.get<MwEntryDto>(`/api/v1/dictionary/mw?slp1Spelling=${slp1Spelling}`);
  },
  getFrischLemma: (lemma: string) => {
    return api.get<FrischEntryDto[]>(`/api/v1/dictionary/frisch?lemma=${encodeURIComponent(lemma)}`);
  },
  getApteLemma: (lemma: string) => {
    return api.get<ApteEntryDto[]>(`/api/v1/dictionary/apte?lemma=${encodeURIComponent(lemma)}`);
  },
  getLemmaSearch: (query: string, limit = 20) => {
    return api.get<LemmaSearchResultDto[]>(
      `/api/v1/dictionary/search/lemma?query=${encodeURIComponent(query)}&limit=${limit}`,
    );
  },
  getDictionaryEntries: (dictionary: string, entryIds: number[]) => {
    const params = new URLSearchParams();
    params.set('dictionary', dictionary);
    entryIds.forEach((id) => params.append('entryIds', String(id)));
    return api.get<DictionaryEntriesResponseDto>(`/api/v1/dictionary/entries?${params.toString()}`);
  },
};
