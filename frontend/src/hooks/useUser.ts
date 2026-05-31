import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { userApi } from '../api/userApi';
import { Locale, Theme } from '../types/user';

export const useMe = () =>
  useQuery({
    queryKey: ['users', 'me'],
    queryFn: userApi.getMe,
    // Keep the user data fresh for a while, but refetch in the background
    staleTime: 1000 * 60 * 5, // 5 minutes
  });

export const useUser = (userId: string) =>
  useQuery({
    queryKey: ['users', userId],
    queryFn: () => userApi.getUser(userId),
    enabled: !!userId,
  });

export const useUserGroups = (userId: string) =>
  useQuery({
    queryKey: ['users', userId, 'groups'],
    queryFn: () => userApi.getUserGroups(userId),
    enabled: !!userId,
  });

export const useUpdateMe = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: { locale?: Locale; theme?: Theme }) => userApi.updateMe(data),
    onSuccess: (updatedUser) => {
      // Update the 'me' query data immediately
      queryClient.setQueryData(['users', 'me'], updatedUser);
    },
  });
};

export const useChangePassword = () => {
    return useMutation({
        mutationFn: ({ currentPassword, newPassword }: any) => 
            userApi.changePassword(currentPassword, newPassword),
    });
};
