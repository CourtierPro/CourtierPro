import { useMutation, useQueryClient } from '@tanstack/react-query';
import axiosInstance from '@/shared/api/axiosInstance';
import { transactionKeys } from '@/features/transactions/api/queries';
import type { TransactionRequestDTO, StageUpdateRequestDTO } from '@/shared/api/types';

export function useUpdateTransactionStage() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async ({ transactionId, data }: { transactionId: string; data: StageUpdateRequestDTO }) => {
            const res = await axiosInstance.patch(
                `/transactions/${transactionId}/stage`,
                data
            );
            return res.data;
        },
        onSuccess: (_data, variables) => {
            queryClient.invalidateQueries({ queryKey: transactionKeys.detail(variables.transactionId) });
            queryClient.invalidateQueries({ queryKey: transactionKeys.lists() });
        },
    });
}

export function useSaveTransactionNotes() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async ({ id, notes }: { id: string; notes: string }) => {
            return { id, notes };
        },
        onSuccess: (_data, variables) => {
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
                payload
            );
            return res.data;
        },
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: transactionKeys.lists() });
        },
    });
}
