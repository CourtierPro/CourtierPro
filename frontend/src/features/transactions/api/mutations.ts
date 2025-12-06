/**
 * Transaction Mutations
 * 
 * Defines TanStack Query mutations for creating and updating transactions.
 * Handles cache invalidation to ensure UI consistency after updates.
 */
import { useMutation, useQueryClient } from '@tanstack/react-query';
import axiosInstance from '@/shared/api/axiosInstance';
import { transactionKeys } from '@/features/transactions/api/queries';
import type { TransactionRequestDTO } from '@/shared/api/types';

export function useUpdateTransactionStage() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async ({ id, stage }: { id: string; stage: number }) => {
            const res = await axiosInstance.patch(
                `/transactions/${id}/stage`,
                { stage },
                { headers: { 'x-broker-id': 'BROKER1' } }
            );
            return res.data;
        },
        onSuccess: (_data, variables) => {
            queryClient.invalidateQueries({ queryKey: transactionKeys.detail(variables.id) });
            queryClient.invalidateQueries({ queryKey: transactionKeys.lists() });
        },
    });
}

export function useSaveTransactionNotes() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async ({ id, notes }: { id: string; notes: string }) => {
            // Simulated API call for notes as per original code
            await new Promise((resolve) => setTimeout(resolve, 500));
            // In real app: await axiosInstance.patch(...)
            return { id, notes };
        },
        onSuccess: (_data, variables) => {
            // In a real app we would invalidate, but since it's simulated we might need to manually update cache or just invalidate
            queryClient.invalidateQueries({ queryKey: transactionKeys.detail(variables.id) });
        },
    });
}

export function useCreateTransaction() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async (payload: TransactionRequestDTO) => {
            const res = await axiosInstance.post(
                '/transactions',
                payload,
                { headers: { 'x-broker-id': 'BROKER1' } }
            );
            return res.data;
        },
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: transactionKeys.lists() });
        },
    });
}
