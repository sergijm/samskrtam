import { useQuery } from '@tanstack/react-query';
import { userApi } from '../api/userApi';
import { UserRole } from '../types/user';

export const useAdminUsers = (
  page: number,
  size: number,
  sortBy: string,
  sortDirection: string,
  search?: string,
  role?: UserRole,
  blocked?: boolean
) =>
  useQuery({
    queryKey: ['admin', 'users', { page, size, sortBy, sortDirection, search, role, blocked }],
    queryFn: async () => {
      const response = await userApi.getAdminUsers(page, size, sortBy, sortDirection, search, role, blocked);
      return response.data; // Return response.data
    },
    keepPreviousData: true, // Keep previous data while fetching new data for pagination/filters
  });
