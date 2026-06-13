import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { userApi } from '../api/userApi';
import { authApi } from '../api/authApi'; // Import authApi for changePassword
import { UpdateProfilePayload } from '../types/user'; // Import the new payload type
import { useAuthStore } from '../store/authStore'; // Import useAuthStore

export const useMe = () => {
  const { isAuthenticated } = useAuthStore(); // Get isAuthenticated from the auth store
  return useQuery({
    queryKey: ['me'],
    queryFn: async () => {
      const response = await userApi.getMe();
      return response.data;
    },
    enabled: !!isAuthenticated, // Only run the query if the user is authenticated
    staleTime: 1000 * 60 * 5, // 5 minutes
  });
};

export const useUser = (userId: string) =>
  useQuery({
    queryKey: ['users', userId],
    queryFn: async () => {
      const response = await userApi.getUser(userId);
      return response.data;
    },
    enabled: !!userId,
  });

export const useUserGroups = (userId: string) =>
  useQuery({
    queryKey: ['users', userId, 'groups'],
    queryFn: async () => {
      const response = await userApi.getUserGroups(userId);
      return response.data;
    },
    enabled: !!userId,
  });

export const useUpdateProfileDetails = () => { // Renamed hook
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (data: UpdateProfilePayload) => {
      const response = await userApi.updateProfileDetails(data);
      return response.data;
    },
    onSuccess: (updatedUser) => {
      // Update the 'me' query data immediately
      queryClient.setQueryData(['me'], updatedUser);
    },
  });
};

export const useGenerateAvatarUploadUrl = () => {
  return useMutation({
    mutationFn: async (contentType: string) => {
      const response = await userApi.generateUploadUrl(contentType);
      return response.data;
    },
  });
};

export const useConfirmAvatarUpload = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (objectKey: string) => {
      const response = await userApi.confirmAvatarUpload(objectKey);
      return response.data;
    },
    onSuccess: (data) => {
      // Invalidate 'me' query to refetch updated avatarUrl
      queryClient.invalidateQueries({ queryKey: ['me'] });
    },
  });
};

export const useChangePassword = () => {
    return useMutation({
        mutationFn: ({ currentPassword, newPassword }: { currentPassword: string; newPassword: string }) =>
            authApi.changePassword(currentPassword, newPassword),
    });
};
