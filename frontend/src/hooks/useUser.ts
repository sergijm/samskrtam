import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { userApi } from '../api/userApi';
import { UpdateProfilePayload } from '../types/user'; // Import the new payload type

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

export const useUpdateProfileDetails = () => { // Renamed hook
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: UpdateProfilePayload) => userApi.updateProfileDetails(data), // Use new API method and payload
    onSuccess: (updatedUser) => {
      // Update the 'me' query data immediately
      queryClient.setQueryData(['users', 'me'], updatedUser);
    },
  });
};

export const useGenerateAvatarUploadUrl = () => {
  return useMutation({
    mutationFn: (contentType: string) => userApi.generateUploadUrl(contentType),
  });
};

export const useConfirmAvatarUpload = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (objectKey: string) => userApi.confirmAvatarUpload(objectKey),
    onSuccess: (data) => {
      // Invalidate 'me' query to refetch updated avatarUrl
      queryClient.invalidateQueries(['users', 'me']);
    },
  });
};

export const useChangePassword = () => {
    return useMutation({
        mutationFn: ({ currentPassword, newPassword }: any) =>
            userApi.changePassword(currentPassword, newPassword),
    });
};
