import { useMutation, useQueryClient } from '@tanstack/react-query';
import axiosInstance from '@/shared/api/axiosInstance';
import { adminKeys } from '@/features/admin/api/queries';
import { dashboardKeys } from '@/features/dashboard/api/queries';
import { setUserActiveStatus } from '@/features/admin/api/adminUserApi';
import {
    deleteResource,
    restoreResource,
    type ResourceType,
} from '@/features/admin/api/adminResourcesApi';
import type { AdminUserResponse } from '@/features/admin/components/InviteUserModal';

export interface InviteUserPayload {
    email: string;
    role: 'BROKER' | 'CLIENT' | 'ADMIN';
    firstName?: string;
    lastName?: string;
    preferredLanguage: 'en' | 'fr';
}

export function useInviteUser() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async (payload: InviteUserPayload) => {
            const res = await axiosInstance.post<AdminUserResponse>('/api/admin/users', payload);
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
            queryClient.invalidateQueries({ queryKey: dashboardKeys.all });
        },
    });
}

export function useDeleteResource() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async ({ type, resourceId }: { type: ResourceType; resourceId: string }) => {
            return deleteResource(type, resourceId);
        },
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: adminKeys.all });
        },
    });
}

export function useRestoreResource() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async ({ type, resourceId }: { type: ResourceType; resourceId: string }) => {
            return restoreResource(type, resourceId);
        },
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: adminKeys.all });
        },
    });
}
