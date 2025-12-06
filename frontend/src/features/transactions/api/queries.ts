import { useQuery } from '@tanstack/react-query';
import axiosInstance from '@/shared/api/axiosInstance';

export const transactionKeys = {
    all: ['transactions'] as const,
    lists: () => [...transactionKeys.all, 'list'] as const,
    details: () => [...transactionKeys.all, 'detail'] as const,
    detail: (id: string) => [...transactionKeys.details(), id] as const,
    client: (clientId: string) => [...transactionKeys.all, 'client', clientId] as const,
};

export interface Transaction {
    transactionId: string;
    clientId: string;
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

export function useTransactions() {
    return useQuery({
        queryKey: transactionKeys.lists(),
        queryFn: async () => {
            const res = await axiosInstance.get<Transaction[]>('/transactions', {
                headers: { 'x-broker-id': 'BROKER1' },
            });
            return res.data;
        },
    });
}

export function useTransaction(id: string | undefined) {
    return useQuery({
        queryKey: transactionKeys.detail(id!),
        queryFn: async () => {
            const res = await axiosInstance.get<Transaction>(`/transactions/${id}`, {
                headers: { 'x-broker-id': 'BROKER1' },
            });
            return res.data;
        },
        enabled: !!id,
    });
}

export function useClientTransactions(clientId: string) {
    return useQuery({
        queryKey: transactionKeys.client(clientId),
        queryFn: async () => {
            const res = await axiosInstance.get<Transaction[]>(`/clients/${clientId}/transactions`);
            return res.data;
        },
        enabled: !!clientId,
    });
}
