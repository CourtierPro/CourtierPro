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

export function useArchiveTransaction() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async (transactionId: string) => {
            await axiosInstance.post(`/transactions/${transactionId}/archive`);
        },
        onSuccess: (_data, transactionId) => {
            queryClient.invalidateQueries({ queryKey: transactionKeys.lists() });
            queryClient.invalidateQueries({ queryKey: transactionKeys.detail(transactionId) });
            queryClient.invalidateQueries({ queryKey: [...transactionKeys.all, 'archived'] });
        },
    });
}

export function useUnarchiveTransaction() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async (transactionId: string) => {
            await axiosInstance.delete(`/transactions/${transactionId}/archive`);
        },
        onSuccess: (_data, transactionId) => {
            queryClient.invalidateQueries({ queryKey: transactionKeys.lists() });
            queryClient.invalidateQueries({ queryKey: transactionKeys.detail(transactionId) });
            queryClient.invalidateQueries({ queryKey: [...transactionKeys.all, 'archived'] });
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

// ==================== PROPERTY MUTATIONS ====================

import { propertyKeys } from '@/features/transactions/api/queries';
import type { PropertyRequestDTO, Property } from '@/shared/api/types';

export function useAddProperty() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async ({ transactionId, data }: { transactionId: string; data: PropertyRequestDTO }) => {
            const res = await axiosInstance.post<Property>(`/transactions/${transactionId}/properties`, data);
            return res.data;
        },
        onSuccess: (_data, variables) => {
            queryClient.invalidateQueries({ queryKey: propertyKeys.all(variables.transactionId) });
            queryClient.invalidateQueries({ queryKey: [...transactionKeys.detail(variables.transactionId), 'timeline'] });
        },
    });
}

export function useUpdateProperty() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async ({ transactionId, propertyId, data }: { transactionId: string; propertyId: string; data: PropertyRequestDTO }) => {
            const res = await axiosInstance.put<Property>(`/transactions/${transactionId}/properties/${propertyId}`, data);
            return res.data;
        },
        onSuccess: (_data, variables) => {
            queryClient.invalidateQueries({ queryKey: propertyKeys.all(variables.transactionId) });
            queryClient.invalidateQueries({ queryKey: propertyKeys.detail(variables.transactionId, variables.propertyId) });
            queryClient.invalidateQueries({ queryKey: [...transactionKeys.detail(variables.transactionId), 'timeline'] });
        },
    });
}

export function useRemoveProperty() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async ({ transactionId, propertyId }: { transactionId: string; propertyId: string }) => {
            await axiosInstance.delete(`/transactions/${transactionId}/properties/${propertyId}`);
        },
        onSuccess: (_data, variables) => {
            queryClient.invalidateQueries({ queryKey: propertyKeys.all(variables.transactionId) });
            queryClient.invalidateQueries({ queryKey: [...transactionKeys.detail(variables.transactionId), 'timeline'] });
        },
    });
}

export function useSetActiveProperty() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async ({ transactionId, propertyId }: { transactionId: string; propertyId: string }) => {
            await axiosInstance.put(`/transactions/${transactionId}/active-property/${propertyId}`);
        },
        onSuccess: (_data, variables) => {
            queryClient.invalidateQueries({ queryKey: transactionKeys.detail(variables.transactionId) });
            queryClient.invalidateQueries({ queryKey: transactionKeys.lists() });
            queryClient.invalidateQueries({ queryKey: [...transactionKeys.detail(variables.transactionId), 'timeline'] });
        },
    });
}

export function useClearActiveProperty() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async ({ transactionId }: { transactionId: string }) => {
            await axiosInstance.delete(`/transactions/${transactionId}/active-property`);
        },
        onSuccess: (_data, variables) => {
            queryClient.invalidateQueries({ queryKey: transactionKeys.detail(variables.transactionId) });
            queryClient.invalidateQueries({ queryKey: transactionKeys.lists() });
            queryClient.invalidateQueries({ queryKey: [...transactionKeys.detail(variables.transactionId), 'timeline'] });
        },
    });
}

// ==================== OFFER MUTATIONS ====================

import { offerKeys } from '@/features/transactions/api/queries';
import type { OfferRequestDTO, Offer } from '@/shared/api/types';

export function useAddOffer() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async ({ transactionId, data }: { transactionId: string; data: OfferRequestDTO }) => {
            const res = await axiosInstance.post<Offer>(`/transactions/${transactionId}/offers`, data);
            return res.data;
        },
        onSuccess: (_data, variables) => {
            queryClient.invalidateQueries({ queryKey: offerKeys.all(variables.transactionId) });
            queryClient.invalidateQueries({ queryKey: [...transactionKeys.detail(variables.transactionId), 'timeline'] });
        },
    });
}

export function useUpdateOffer() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async ({ transactionId, offerId, data }: { transactionId: string; offerId: string; data: OfferRequestDTO }) => {
            const res = await axiosInstance.put<Offer>(`/transactions/${transactionId}/offers/${offerId}`, data);
            return res.data;
        },
        onSuccess: (_data, variables) => {
            queryClient.invalidateQueries({ queryKey: offerKeys.all(variables.transactionId) });
            queryClient.invalidateQueries({ queryKey: offerKeys.detail(variables.transactionId, variables.offerId) });
            queryClient.invalidateQueries({ queryKey: [...transactionKeys.detail(variables.transactionId), 'timeline'] });
        },
    });
}

export function useRemoveOffer() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async ({ transactionId, offerId }: { transactionId: string; offerId: string }) => {
            await axiosInstance.delete(`/transactions/${transactionId}/offers/${offerId}`);
        },
        onSuccess: (_data, variables) => {
            queryClient.invalidateQueries({ queryKey: offerKeys.all(variables.transactionId) });
            queryClient.invalidateQueries({ queryKey: [...transactionKeys.detail(variables.transactionId), 'timeline'] });
        },
    });
}

// ==================== CONDITION MUTATIONS ====================

import { conditionKeys } from '@/features/transactions/api/queries';
import type { ConditionRequestDTO, Condition, ConditionStatus } from '@/shared/api/types';

export function useAddCondition() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async ({ transactionId, data }: { transactionId: string; data: ConditionRequestDTO }) => {
            const res = await axiosInstance.post<Condition>(`/transactions/${transactionId}/conditions`, data);
            return res.data;
        },
        onSuccess: (_data, variables) => {
            queryClient.invalidateQueries({ queryKey: conditionKeys.all(variables.transactionId) });
            queryClient.invalidateQueries({ queryKey: [...transactionKeys.detail(variables.transactionId), 'timeline'] });
        },
    });
}

export function useUpdateCondition() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async ({ transactionId, conditionId, data }: { transactionId: string; conditionId: string; data: ConditionRequestDTO }) => {
            const res = await axiosInstance.put<Condition>(`/transactions/${transactionId}/conditions/${conditionId}`, data);
            return res.data;
        },
        onSuccess: (_data, variables) => {
            queryClient.invalidateQueries({ queryKey: conditionKeys.all(variables.transactionId) });
            queryClient.invalidateQueries({ queryKey: [...transactionKeys.detail(variables.transactionId), 'timeline'] });
        },
    });
}

export function useRemoveCondition() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async ({ transactionId, conditionId }: { transactionId: string; conditionId: string }) => {
            await axiosInstance.delete(`/transactions/${transactionId}/conditions/${conditionId}`);
        },
        onSuccess: (_data, variables) => {
            queryClient.invalidateQueries({ queryKey: conditionKeys.all(variables.transactionId) });
            queryClient.invalidateQueries({ queryKey: [...transactionKeys.detail(variables.transactionId), 'timeline'] });
        },
    });
}

export function useUpdateConditionStatus() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async ({ transactionId, conditionId, status }: { transactionId: string; conditionId: string; status: ConditionStatus }) => {
            const res = await axiosInstance.put<Condition>(`/transactions/${transactionId}/conditions/${conditionId}/status`, null, {
                params: { status }
            });
            return res.data;
        },
        onSuccess: (_data, variables) => {
            queryClient.invalidateQueries({ queryKey: conditionKeys.all(variables.transactionId) });
            queryClient.invalidateQueries({ queryKey: [...transactionKeys.detail(variables.transactionId), 'timeline'] });
        },
    });
}
