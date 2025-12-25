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
            queryClient.invalidateQueries({ queryKey: [...transactionKeys.detail(variables.transactionId), 'timeline'] }); // Ajouté
        },
    });
}

export function useSaveTransactionNotes() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async ({ id, notes }: { id: string; notes: string }) => {
            // Call backend endpoint to save internal notes and create timeline event
            await axiosInstance.post(`/transactions/${id}/internal-notes`, notes, {
                headers: { 'Content-Type': 'text/plain' },
            });
            return { id, notes };
        },
        onSuccess: (_data, variables) => {
            queryClient.invalidateQueries({ queryKey: transactionKeys.detail(variables.id) });
            queryClient.invalidateQueries({ queryKey: [...transactionKeys.detail(variables.id), 'timeline'] });
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

export function usePinTransaction() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async (transactionId: string) => {
            await axiosInstance.post(`/transactions/${transactionId}/pin`);
        },
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: transactionKeys.pinned() });
        },
    });
}

export function useUnpinTransaction() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async (transactionId: string) => {
            await axiosInstance.delete(`/transactions/${transactionId}/pin`);
        },
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: transactionKeys.pinned() });
        },
    });
}

export function useAddParticipant() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async ({ transactionId, data }: { transactionId: string; data: { name: string; role: string; email?: string; phoneNumber?: string } }) => {
            await axiosInstance.post(`/transactions/${transactionId}/participants`, data);
        },
        onSuccess: (_data, variables) => {
            queryClient.invalidateQueries({ queryKey: transactionKeys.participants(variables.transactionId) });
        },
    });
}

export function useRemoveParticipant() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async ({ transactionId, participantId }: { transactionId: string; participantId: string }) => {
            await axiosInstance.delete(`/transactions/${transactionId}/participants/${participantId}`);
        },
        onSuccess: (_data, variables) => {
            queryClient.invalidateQueries({ queryKey: transactionKeys.participants(variables.transactionId) });
        },
    });
}
