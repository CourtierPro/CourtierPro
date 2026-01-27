import { updateDocumentRequest } from './documentsApi';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { documentKeys } from '@/features/documents/api/queries';
import { createDocumentRequest, submitDocument, reviewDocument, sendDocumentReminder, type CreateDocumentRequestDTO } from './documentsApi.ts';

import type { UpdateDocumentRequestDTO } from './documentsApi';
export function useUpdateDocumentRequest() {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: async ({ transactionId, requestId, data }: { transactionId: string; requestId: string; data: UpdateDocumentRequestDTO }) => {
            return updateDocumentRequest(transactionId, requestId, data);
        },
        onSuccess: (_, { transactionId }) => {
            queryClient.invalidateQueries({ queryKey: documentKeys.outstanding() });
            queryClient.invalidateQueries({ queryKey: ['documents', transactionId] });
            queryClient.invalidateQueries({ queryKey: ['documents'] });
        },
    });
}


export function useRequestDocument() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: ({ transactionId, data }: { transactionId: string; data: CreateDocumentRequestDTO }) =>
            createDocumentRequest(transactionId, data),
        onSuccess: (_, { transactionId }) => {
            queryClient.invalidateQueries({ queryKey: documentKeys.outstanding() });
            queryClient.invalidateQueries({ queryKey: documentKeys.list(transactionId) });
            queryClient.invalidateQueries({ queryKey: ['documents'] });
            queryClient.invalidateQueries({
                queryKey: [
                    'transactions', 'detail', transactionId, 'timeline'
                ]
            }); // Timeline broker
            queryClient.invalidateQueries({
                queryKey: [
                    'transaction', transactionId, 'timeline', 'client'
                ]
            }); // Timeline client
        },
    });
}

export function useSubmitDocument() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: ({ transactionId, requestId, file }: { transactionId: string; requestId: string; file: File }) =>
            submitDocument(transactionId, requestId, file),
        onSuccess: (_, { transactionId }) => {
            queryClient.invalidateQueries({ queryKey: documentKeys.outstanding() });
            queryClient.invalidateQueries({ queryKey: documentKeys.list(transactionId) });
            queryClient.invalidateQueries({ queryKey: ['documents'] });
            queryClient.invalidateQueries({
                queryKey: [
                    'transactions', 'detail', transactionId, 'timeline'
                ]
            }); // Timeline broker
            queryClient.invalidateQueries({
                queryKey: [
                    'transaction', transactionId, 'timeline', 'client'
                ]
            }); // Timeline client
        },
    });
}

export const useReviewDocument = () => {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: ({ transactionId, requestId, decision, comments }: {
            transactionId: string;
            requestId: string;
            decision: 'APPROVED' | 'NEEDS_REVISION';
            comments?: string;
        }) => reviewDocument(transactionId, requestId, decision, comments),
        onSuccess: (_, { transactionId }) => {
            queryClient.invalidateQueries({ queryKey: documentKeys.outstanding() });
            queryClient.invalidateQueries({ queryKey: documentKeys.list(transactionId) });
            queryClient.invalidateQueries({ queryKey: documentKeys.stat(transactionId) });
            queryClient.invalidateQueries({ queryKey: ['documents'] });
            queryClient.invalidateQueries({
                queryKey: [
                    'transactions', 'detail', transactionId, 'timeline'
                ]
            }); // Timeline broker
            queryClient.invalidateQueries({
                queryKey: [
                    'transaction', transactionId, 'timeline', 'client'
                ]
            }); // Timeline client
        },
    });
};

export const useSendDocumentReminder = () => {
    return useMutation({
        mutationFn: (requestId: string) => sendDocumentReminder(requestId),
    });
};

