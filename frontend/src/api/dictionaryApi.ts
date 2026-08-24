import api from './axios';
import { MwEntryDto, FrischEntryDto, ApteEntryDto } from '../types';

export const dictionaryApi = {
  searchByLemma: (query: string) => {
    return api.get<MwEntryDto>(`/api/v1/dictionary/search?query=${query}`);
  },
  getMwEntry: (slp1Spelling: string) => {
    return api.get<MwEntryDto>(`/api/v1/dictionary/entry?slp1Spelling=${slp1Spelling}`);
  },
  getFrischLemma: (lemma: string) => {
    return api.get<FrischEntryDto[]>(`/api/v1/dictionary/frisch?lemma=${encodeURIComponent(lemma)}`);
  },
  getApteLemma: (lemma: string) => {
    return api.get<ApteEntryDto[]>(`/api/v1/dictionary/apte?lemma=${encodeURIComponent(lemma)}`);
  },
};
