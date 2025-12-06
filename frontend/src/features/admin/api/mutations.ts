import { useMutation, useQueryClient } from '@tanstack/react-query';
import axiosInstance from '@/shared/api/axiosInstance';
import { adminKeys } from '@/features/admin/api/queries';
import { setUserActiveStatus } from '@/features/admin/api/adminUserApi';
import type { AdminUserResponse } from '@/features/admin/components/InviteUserModal';

export interface InviteUserPayload {
    email: string;
    role: 'BROKER' | 'CLIENT' | 'ADMIN';
    firstName?: string;
    lastName?: string;
}

export function useInviteUser() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async (payload: InviteUserPayload) => {
            const res = await axiosInstance.post<AdminUserResponse>('/api/admin/invite', payload);
            return res.data;
        },
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: adminKeys.users() });
        },
    });
}

export function useSetUserActiveStatus() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async ({ userId, active }: { userId: string; active: boolean }) => {
            return setUserActiveStatus(userId, active);
        },
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: adminKeys.users() });
        },
    });
}
