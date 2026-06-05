import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { userApi } from '../api/userApi';

export const useGroups = () =>
  useQuery({
    queryKey: ['groups'],
    queryFn: async () => {
      const response = await userApi.getGroups();
      return response.data; // Return response.data
    },
  });

export const useGroup = (groupId: string) =>
  useQuery({
    queryKey: ['groups', groupId],
    queryFn: async () => {
      const response = await userApi.getGroup(groupId);
      return response.data; // Return response.data
    },
    enabled: !!groupId,
  });

export const useCreateGroup = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (name: string) => {
      const response = await userApi.createGroup(name);
      return response.data; // Return response.data
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['groups'] });
    },
  });
};

export const useRenameGroup = (groupId: string) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (name: string) => {
      const response = await userApi.renameGroup(groupId, name);
      return response.data; // Return response.data
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['groups', groupId] });
      queryClient.invalidateQueries({ queryKey: ['groups'] });
    },
  });
};

export const useAddMember = (groupId: string) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (userId: string) => userApi.addMember(groupId, userId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['groups', groupId] });
    },
  });
};

export const useRemoveMember = (groupId: string) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (userId: string) => userApi.removeMember(groupId, userId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['groups', groupId] });
    },
  });
};

export const useSetCurator = (groupId: string) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (userId: string) => userApi.setCurator(groupId, userId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['groups', groupId] });
    },
  });
};
