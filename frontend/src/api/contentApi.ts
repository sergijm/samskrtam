import api from './axios';
import { SandhiRuleDto, EamenauExerciseDto, EamenauExerciseDetailDto, SolutionDto, SandhiRuleInfo, SolutionUpdateRequestDto } from '../types';

export const contentApi = {
  getAllSandhiRules: () => {
    return api.get<SandhiRuleDto[]>('/api/v1/eamenau/sandhi-rules');
  },
  getAllEamenauExercises: () => {
    return api.get<EamenauExerciseDto[]>('/api/v1/eamenau/exercises');
  },
  getEamenauExerciseById: (id: string) => {
    return api.get<EamenauExerciseDetailDto>(`/api/v1/eamenau/exercises/${id}`);
  },
  getSolutionsForTask: (taskId: number) => {
    return api.get<SolutionDto[]>(`/api/v1/eamenau/exercises/tasks/${taskId}/solution`);
  },
  getUniqueSandhiRulesForExercise: (exerciseId: number) => {
    return api.get<SandhiRuleInfo[]>(`/api/v1/eamenau/exercises/${exerciseId}/sandhi-rules`);
  },
  updateSolution: (solutionId: number, stepByStep: string, ruleNumbers: string) => {
    const data: SolutionUpdateRequestDto = { stepByStep, ruleNumbers };
    return api.put(`/api/v1/eamenau/exercises/solutions/${solutionId}`, data);
  }
};
