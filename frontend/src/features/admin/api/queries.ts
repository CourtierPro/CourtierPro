import { useQuery } from '@tanstack/react-query';
import { getAdminUsers } from '@/features/admin/api/adminUserApi';
import {
    listResources,
    previewDeletion,
    getAuditHistory,
    type ResourceType,
} from '@/features/admin/api/adminResourcesApi';

export const adminKeys = {
    all: ['admin'] as const,
    users: () => [...adminKeys.all, 'users'] as const,
    resources: (type: ResourceType, includeDeleted?: boolean) =>
        [...adminKeys.all, 'resources', type, includeDeleted] as const,
    resourcePreview: (type: ResourceType, resourceId: string) =>
        [...adminKeys.all, 'resources', type, resourceId, 'preview'] as const,
    auditHistory: () => [...adminKeys.all, 'audit-history'] as const,
};

export function useAdminUsers() {
    return useQuery({
        queryKey: adminKeys.users(),
        queryFn: getAdminUsers,
    });
}

export function useAdminResources(type: ResourceType, includeDeleted = false) {
    return useQuery({
        queryKey: adminKeys.resources(type, includeDeleted),
        queryFn: () => listResources(type, includeDeleted),
    });
}

export function useResourcePreview(type: ResourceType, resourceId: string | null) {
    return useQuery({
        queryKey: adminKeys.resourcePreview(type, resourceId || ''),
        queryFn: () => previewDeletion(type, resourceId!),
        enabled: !!resourceId,
    });
}

export function useAuditHistory() {
    return useQuery({
        queryKey: adminKeys.auditHistory(),
        queryFn: getAuditHistory,
    });
}
