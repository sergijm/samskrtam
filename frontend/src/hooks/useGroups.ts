import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { userApi } from '../api/userApi';

export const useGroups = () =>
  useQuery({
    queryKey: ['groups'],
    queryFn: userApi.getGroups,
  });

export const useGroup = (groupId: string) =>
  useQuery({
    queryKey: ['groups', groupId],
    queryFn: () => userApi.getGroup(groupId),
    enabled: !!groupId,
  });

export const useCreateGroup = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (name: string) => userApi.createGroup(name),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['groups'] });
    },
  });
};

export const useRenameGroup = (groupId: string) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (name: string) => userApi.renameGroup(groupId, name),
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
