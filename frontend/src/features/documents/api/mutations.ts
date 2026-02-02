import { updateDocument } from './documentsApi';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { documentKeys } from '@/features/documents/api/queries';
import { createDocument, submitDocument, reviewDocument, sendDocumentReminder, type CreateDocumentDTO } from './documentsApi.ts';

import type { UpdateDocumentDTO } from './documentsApi';

export function useUpdateDocument() {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: async ({ transactionId, documentId, data }: { transactionId: string; documentId: string; data: UpdateDocumentDTO }) => {
            return updateDocument(transactionId, documentId, data);
        },
        onSuccess: (_, { transactionId }) => {
            queryClient.invalidateQueries({ queryKey: documentKeys.outstanding() });
            queryClient.invalidateQueries({ queryKey: ['documents', transactionId] });
            queryClient.invalidateQueries({ queryKey: ['documents'] });
        },
    });
}


export function useCreateDocument() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: ({ transactionId, data }: { transactionId: string; data: CreateDocumentDTO }) =>
            createDocument(transactionId, data),
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
        mutationFn: ({ transactionId, documentId, file }: { transactionId: string; documentId: string; file: File }) =>
            submitDocument(transactionId, documentId, file),
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
        mutationFn: ({ transactionId, documentId, decision, comments }: {
            transactionId: string;
            documentId: string;
            decision: 'APPROVED' | 'NEEDS_REVISION';
            comments?: string;
        }) => reviewDocument(transactionId, documentId, decision, comments),
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
        mutationFn: (documentId: string) => sendDocumentReminder(documentId),
    });
};
