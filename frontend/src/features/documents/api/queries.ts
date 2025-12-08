import { useQuery } from '@tanstack/react-query';
import { fetchDocuments } from './documentsApi';
import type { DocumentRequest } from '../types';

export const documentKeys = {
    all: ['documents'] as const,
    lists: () => [...documentKeys.all, 'list'] as const,
    list: (transactionId: string) => [...documentKeys.lists(), transactionId] as const,
    details: () => [...documentKeys.all, 'detail'] as const,
    detail: (id: string) => [...documentKeys.details(), id] as const,
};

export function useDocuments(transactionId: string) {
    return useQuery({
        queryKey: documentKeys.list(transactionId),
        queryFn: () => fetchDocuments(transactionId),
        enabled: !!transactionId,
    });
}

export type { DocumentRequest as Document }; // Alias for compatibility if needed, but better to migrate
