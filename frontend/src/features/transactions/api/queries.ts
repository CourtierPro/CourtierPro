import { useQuery } from '@tanstack/react-query';
import axiosInstance from '@/shared/api/axiosInstance';
import type { TimelineEntryDTO } from '@/shared/api/types';

// Re-export a local type alias expected by components
export type TimelineEntry = TimelineEntryDTO;

export const transactionKeys = {
    all: ['transactions'] as const,
    lists: () => [...transactionKeys.all, 'list'] as const,
    details: () => [...transactionKeys.all, 'detail'] as const,
    detail: (id: string) => [...transactionKeys.details(), id] as const,
    client: (clientId: string) => [...transactionKeys.all, 'client', clientId] as const,
    pinned: () => [...transactionKeys.all, 'pinned'] as const,
};

export interface Transaction {
    transactionId: string;
    clientId: string;
    clientName: string;
    propertyAddress: {
        street: string;
        city: string;
        province: string;
        postalCode: string;
    };
    side: 'BUY_SIDE' | 'SELL_SIDE';
    currentStage: number;
    totalStages: number;
    status: 'active' | 'closed' | 'terminated';
    openedAt?: string;
    openedDate?: string;
    notes?: string;
    brokerId?: string;
}

export function usePinnedTransactionIds() {
    return useQuery({
        queryKey: transactionKeys.pinned(),
        queryFn: async () => {
            const res = await axiosInstance.get<string[]>('/transactions/pinned');
            return new Set(res.data);
        },
    });
}

export function useTransactions(filters?: { status?: string; stage?: string; side?: string }) {
    return useQuery({
        queryKey: [...transactionKeys.lists(), filters],
        queryFn: async () => {
            const params = new URLSearchParams();
            if (filters?.status && filters.status !== 'all') params.append('status', filters.status);
            if (filters?.stage && filters.stage !== 'all') params.append('stage', filters.stage);
            if (filters?.side && filters.side !== 'all') params.append('side', filters.side);

            const res = await axiosInstance.get<Transaction[]>(`/transactions?${params.toString()}`);
            return res.data;
        },
    });
}

export function useTransaction(id: string | undefined) {
    return useQuery({
        queryKey: transactionKeys.detail(id!),
        queryFn: async () => {
            const res = await axiosInstance.get<Transaction>(`/transactions/${id}`);
            return res.data;
        },
        enabled: !!id,
    });
}

export function useClientTransactions(clientId: string) {
    return useQuery({
        queryKey: transactionKeys.client(clientId),
        queryFn: async () => {
            const res = await axiosInstance.get<Transaction[]>(`/clients/${encodeURIComponent(clientId)}/transactions`);
            return res.data;
        },
        enabled: !!clientId,
    });
}
