import api from './axios';
import { MwWordSearchDto, MwEntryDto } from '../types';

export const dictionaryApi = {
  searchMwWords: (query: string) => {
    return api.get<MwWordSearchDto[]>(`/api/v1/mw-dictionary/search?query=${query}`);
  },
  getMwEntry: (slp1Spelling: string) => {
    return api.get<MwEntryDto>(`/api/v1/mw-dictionary/entry?slp1Spelling=${slp1Spelling}`);
  },
};
