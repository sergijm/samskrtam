import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { userApi } from '../api/userApi';
import { authApi } from '../api/authApi'; // Import authApi for changePassword
import { UpdateProfilePayload } from '../types/user'; // Import the new payload type

export const useMe = () =>
  useQuery({
    queryKey: ['users', 'me'],
    queryFn: async () => {
      const response = await userApi.getMe();
      return response.data; // Return response.data
    },
    // Keep the user data fresh for a while, but refetch in the background
    staleTime: 1000 * 60 * 5, // 5 minutes
  });

export const useUser = (userId: string) =>
  useQuery({
    queryKey: ['users', userId],
    queryFn: async () => {
      const response = await userApi.getUser(userId);
      return response.data; // Return response.data
    },
    enabled: !!userId,
  });

export const useUserGroups = (userId: string) =>
  useQuery({
    queryKey: ['users', userId, 'groups'],
    queryFn: async () => {
      const response = await userApi.getUserGroups(userId);
      return response.data; // Return response.data
    },
    enabled: !!userId,
  });

export const useUpdateProfileDetails = () => { // Renamed hook
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (data: UpdateProfilePayload) => {
      const response = await userApi.updateProfileDetails(data);
      return response.data; // Return response.data
    },
    onSuccess: (updatedUser) => {
      // Update the 'me' query data immediately
      queryClient.setQueryData(['users', 'me'], updatedUser);
    },
  });
};

export const useGenerateAvatarUploadUrl = () => {
  return useMutation({
    mutationFn: async (contentType: string) => {
      const response = await userApi.generateUploadUrl(contentType);
      return response.data; // Return response.data
    },
  });
};

export const useConfirmAvatarUpload = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (objectKey: string) => {
      const response = await userApi.confirmAvatarUpload(objectKey);
      return response.data; // Return response.data
    },
    onSuccess: (data) => {
      // Invalidate 'me' query to refetch updated avatarUrl
      queryClient.invalidateQueries({ queryKey: ['users', 'me'] });
    },
  });
};

export const useChangePassword = () => {
    return useMutation({
        mutationFn: ({ currentPassword, newPassword }: { currentPassword: string; newPassword: string }) =>
            authApi.changePassword(currentPassword, newPassword),
    });
};
