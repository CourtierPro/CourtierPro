/**
 * Admin Queries
 * 
 * Defines TanStack Query hooks for fetching admin-related data (e.g., users).
 */
import { useQuery } from '@tanstack/react-query';
import { getAdminUsers } from '@/features/admin/api/adminUserApi';

export const adminKeys = {
    all: ['admin'] as const,
    users: () => [...adminKeys.all, 'users'] as const,
};

export function useAdminUsers() {
    return useQuery({
        queryKey: adminKeys.users(),
        queryFn: getAdminUsers,
    });
}
