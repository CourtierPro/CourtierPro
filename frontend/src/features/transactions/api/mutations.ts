import { useMutation, useQueryClient } from '@tanstack/react-query';
import axiosInstance from '@/shared/api/axiosInstance';
import { transactionKeys } from '@/features/transactions/api/queries';
import { dashboardKeys } from '@/features/dashboard/api/queries';
import { documentKeys } from '@/features/documents/api/queries';
import type { TransactionRequestDTO, StageUpdateRequestDTO, AddParticipantRequestDTO, UpdateParticipantRequestDTO } from '@/shared/api/types';

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
            queryClient.invalidateQueries({ queryKey: [...transactionKeys.detail(variables.transactionId), 'timeline'] });
            queryClient.invalidateQueries({ queryKey: documentKeys.list(variables.transactionId) });
            queryClient.invalidateQueries({ queryKey: documentKeys.stat(variables.transactionId) });
            queryClient.invalidateQueries({ queryKey: documentKeys.checklists() });
            // Invalidate dashboard recent activity
            queryClient.invalidateQueries({ queryKey: dashboardKeys.all });
        },
    });
}

export function useTerminateTransaction() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async ({ transactionId, reason }: { transactionId: string; reason: string }) => {
            const res = await axiosInstance.post(
                `/transactions/${transactionId}/terminate`,
                { reason }
            );
            return res.data;
        },
        onSuccess: (_data, variables) => {
            queryClient.invalidateQueries({ queryKey: transactionKeys.detail(variables.transactionId) });
            queryClient.invalidateQueries({ queryKey: transactionKeys.lists() });
            queryClient.invalidateQueries({ queryKey: [...transactionKeys.detail(variables.transactionId), 'timeline'] });
            queryClient.invalidateQueries({ queryKey: dashboardKeys.all });
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
            // Invalidate dashboard recent activity so it updates immediately
            queryClient.invalidateQueries({ queryKey: dashboardKeys.all });
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
        mutationFn: async ({ transactionId, data }: { transactionId: string; data: AddParticipantRequestDTO }) => {
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

export function useUpdateParticipant() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async ({ transactionId, participantId, data }: { transactionId: string; participantId: string; data: UpdateParticipantRequestDTO }) => {
            await axiosInstance.put(`/transactions/${transactionId}/participants/${participantId}`, data);
        },
        onSuccess: (_data, variables) => {
            queryClient.invalidateQueries({ queryKey: transactionKeys.participants(variables.transactionId) });
            queryClient.invalidateQueries({ queryKey: transactionKeys.detail(variables.transactionId) });
        },
    });
}

export function useUpdateParticipantPermissions() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async ({ transactionId, participantId, permissions }: { transactionId: string; participantId: string; permissions: string[] }) => {
            const res = await axiosInstance.put(
                `/transactions/${transactionId}/participants/${participantId}/permissions`,
                permissions
            );
            return res.data;
        },
        onSuccess: (_data, variables) => {
            queryClient.invalidateQueries({ queryKey: transactionKeys.participants(variables.transactionId) });
            queryClient.invalidateQueries({ queryKey: transactionKeys.detail(variables.transactionId) });
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

export function useUpdatePropertyStatus() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async ({ transactionId, propertyId, status, notes }: { transactionId: string; propertyId: string; status: string; notes?: string }) => {
            const res = await axiosInstance.patch<Property>(`/transactions/${transactionId}/properties/${propertyId}/status`, notes, {
                params: { status },
                headers: { 'Content-Type': 'text/plain' }
            });
            return res.data;
        },
        onSuccess: (_data, variables) => {
            queryClient.invalidateQueries({ queryKey: propertyKeys.all(variables.transactionId) });
            queryClient.invalidateQueries({ queryKey: propertyKeys.detail(variables.transactionId, variables.propertyId) });
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
            // Invalidate dashboard so broker dashboard updates immediately
            queryClient.invalidateQueries({ queryKey: dashboardKeys.all });
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
            // Invalidate dashboard so broker dashboard updates immediately
            queryClient.invalidateQueries({ queryKey: dashboardKeys.all });
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
            // Invalidate dashboard so broker dashboard updates immediately
            queryClient.invalidateQueries({ queryKey: dashboardKeys.all });
        },
    });
}

// ==================== CLIENT OFFER DECISION MUTATION ====================

import type { ClientOfferDecisionDTO, Offer as OfferType } from '@/shared/api/types';

export function useSubmitOfferDecision() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async ({ clientId, transactionId, offerId, data }: { clientId: string; transactionId: string; offerId: string; data: ClientOfferDecisionDTO }) => {
            const res = await axiosInstance.put<OfferType>(`/clients/${clientId}/transactions/${transactionId}/offers/${offerId}/decision`, data);
            return res.data;
        },
        onSuccess: (_data, variables) => {
            queryClient.invalidateQueries({ queryKey: offerKeys.all(variables.transactionId) });
            queryClient.invalidateQueries({ queryKey: offerKeys.detail(variables.transactionId, variables.offerId) });
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

// ==================== PROPERTY OFFER MUTATIONS ====================

import { propertyOfferKeys, offerDocumentKeys } from '@/features/transactions/api/queries';
import type { PropertyOfferRequestDTO, PropertyOffer, OfferDocument } from '@/shared/api/types';

export function useAddPropertyOffer() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async ({ propertyId, data }: { propertyId: string; transactionId: string; data: PropertyOfferRequestDTO }) => {
            const res = await axiosInstance.post<PropertyOffer>(`/transactions/properties/${propertyId}/offers`, data);
            return res.data;
        },
        onSuccess: (_data, variables) => {
            queryClient.invalidateQueries({ queryKey: propertyOfferKeys.all(variables.propertyId) });
            queryClient.invalidateQueries({ queryKey: propertyKeys.all(variables.transactionId) });
            queryClient.invalidateQueries({ queryKey: [...transactionKeys.detail(variables.transactionId), 'timeline'] });
        },
    });
}

export function useUpdatePropertyOffer() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async ({ propertyId, propertyOfferId, data }: { propertyId: string; propertyOfferId: string; transactionId: string; data: PropertyOfferRequestDTO }) => {
            const res = await axiosInstance.put<PropertyOffer>(`/transactions/properties/${propertyId}/offers/${propertyOfferId}`, data);
            return res.data;
        },
        onSuccess: (_data, variables) => {
            queryClient.invalidateQueries({ queryKey: propertyOfferKeys.all(variables.propertyId) });
            queryClient.invalidateQueries({ queryKey: propertyOfferKeys.detail(variables.propertyId, variables.propertyOfferId) });
            queryClient.invalidateQueries({ queryKey: propertyKeys.all(variables.transactionId) });
            queryClient.invalidateQueries({ queryKey: [...transactionKeys.detail(variables.transactionId), 'timeline'] });
        },
    });
}

export function useUploadOfferDocument() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async ({ transactionId, offerId, file }: { transactionId: string; offerId: string; file: File }) => {
            const formData = new FormData();
            formData.append('file', file);
            const res = await axiosInstance.post<OfferDocument>(`/transactions/${transactionId}/offers/${offerId}/documents`, formData, {
                headers: { 'Content-Type': 'multipart/form-data' },
            });
            return res.data;
        },
        onSuccess: (_data, variables) => {
            queryClient.invalidateQueries({ queryKey: offerDocumentKeys.all(variables.offerId) });
            queryClient.invalidateQueries({ queryKey: offerKeys.detail(variables.transactionId, variables.offerId) });
            queryClient.invalidateQueries({ queryKey: [...transactionKeys.detail(variables.transactionId), 'timeline'] });
        },
    });
}

export function useUploadPropertyOfferDocument() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async ({ propertyId, propertyOfferId, file }: { propertyId: string; propertyOfferId: string; transactionId: string; file: File }) => {
            const formData = new FormData();
            formData.append('file', file);
            const res = await axiosInstance.post<OfferDocument>(`/transactions/properties/${propertyId}/offers/${propertyOfferId}/documents`, formData, {
                headers: { 'Content-Type': 'multipart/form-data' },
            });
            return res.data;
        },
        onSuccess: (_data, variables) => {
            queryClient.invalidateQueries({ queryKey: propertyOfferKeys.documents(variables.propertyOfferId) });
            queryClient.invalidateQueries({ queryKey: propertyOfferKeys.all(variables.propertyId) });
            queryClient.invalidateQueries({ queryKey: [...transactionKeys.detail(variables.transactionId), 'timeline'] });
        },
    });
}

export function useDeleteOfferDocument() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async ({ documentId }: { documentId: string; offerId?: string; propertyOfferId?: string; transactionId: string }) => {
            await axiosInstance.delete(`/transactions/documents/${documentId}`);
        },
        onSuccess: (_data, variables) => {
            if (variables.offerId) {
                queryClient.invalidateQueries({ queryKey: offerDocumentKeys.all(variables.offerId) });
            }
            if (variables.propertyOfferId) {
                queryClient.invalidateQueries({ queryKey: propertyOfferKeys.documents(variables.propertyOfferId) });
            }
            queryClient.invalidateQueries({ queryKey: [...transactionKeys.detail(variables.transactionId), 'timeline'] });
        },
    });
}

// ==================== SEARCH CRITERIA MUTATIONS ====================

import { searchCriteriaKeys } from '@/features/transactions/api/queries';
import type { SearchCriteriaRequestDTO, SearchCriteria } from '@/shared/api/types';

export function useUpdateSearchCriteria() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async ({ transactionId, data }: { transactionId: string; data: SearchCriteriaRequestDTO }) => {
            const res = await axiosInstance.put<SearchCriteria>(`/transactions/${transactionId}/search-criteria`, data);
            return res.data;
        },
        onSuccess: (_data, variables) => {
            queryClient.invalidateQueries({ queryKey: searchCriteriaKeys.byTransaction(variables.transactionId) });
        },
    });
}

export function useDeleteSearchCriteria() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async (transactionId: string) => {
            await axiosInstance.delete(`/transactions/${transactionId}/search-criteria`);
        },
        onSuccess: (_data, transactionId) => {
            queryClient.invalidateQueries({ queryKey: searchCriteriaKeys.byTransaction(transactionId) });
        },
    });
}
