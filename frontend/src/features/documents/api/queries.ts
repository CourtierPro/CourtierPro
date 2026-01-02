import { useQuery } from '@tanstack/react-query';
import { fetchDocuments } from './documentsApi';
import type { DocumentRequest } from '../types';
import axiosInstance from '@/shared/api/axiosInstance';

export const documentKeys = {
    all: ['documents'] as const,
    lists: () => [...documentKeys.all, 'list'] as const,
    list: (transactionId: string) => [...documentKeys.lists(), transactionId] as const,
    details: () => [...documentKeys.all, 'detail'] as const,
    detail: (id: string) => [...documentKeys.details(), id] as const,
    stats: () => [...documentKeys.all, 'stats'] as const,
    stat: (transactionId: string) => [...documentKeys.stats(), transactionId] as const,
};

export function useDocuments(transactionId: string) {
    return useQuery({
        queryKey: documentKeys.list(transactionId),
        queryFn: () => fetchDocuments(transactionId),
        enabled: !!transactionId,
    });
}

export function useDocumentStats(transactionId: string) {
    return useQuery({
        queryKey: documentKeys.stat(transactionId),
        queryFn: async () => {
            const response = await axiosInstance.get(`/transactions/${transactionId}/documents`);
            const docs = response.data || [];
            
            const pending = docs.filter((d: any) => d.status === 'REQUESTED').length;
            const submitted = docs.filter((d: any) => d.status === 'SUBMITTED').length;
            const approved = docs.filter((d: any) => d.status === 'APPROVED').length;
            const needsRevision = docs.filter((d: any) => d.status === 'NEEDS_REVISION').length;
            
            return {
                count: docs.length,
                statuses: { pending, submitted, approved, needsRevision }
            };
        },
        enabled: !!transactionId,
    });
}

export type { DocumentRequest as Document }; // Alias for compatibility if needed, but better to migrate
