import { updateDocument } from './documentsApi';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { documentKeys } from '@/features/documents/api/queries';
import { createDocument, submitDocument, reviewDocument, sendDocumentReminder, sendDocumentRequest, deleteDocument, uploadFileToDocument, shareDocumentWithClient, updateChecklistManualState, type CreateDocumentDTO } from './documentsApi.ts';

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
            queryClient.invalidateQueries({ queryKey: documentKeys.checklists() });
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
            queryClient.invalidateQueries({ queryKey: documentKeys.checklists() });
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
            queryClient.invalidateQueries({ queryKey: documentKeys.checklists() });
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

/**
 * Mutation hook to upload a file to a document without changing its status.
 * Used for attaching files to draft documents.
 */
export function useUploadFileToDocument() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: ({ transactionId, documentId, file }: { transactionId: string; documentId: string; file: File }) =>
            uploadFileToDocument(transactionId, documentId, file),
        onSuccess: (_, { transactionId }) => {
            queryClient.invalidateQueries({ queryKey: documentKeys.list(transactionId) });
            queryClient.invalidateQueries({ queryKey: ['documents'] });
            queryClient.invalidateQueries({ queryKey: documentKeys.checklists() });
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
            queryClient.invalidateQueries({ queryKey: documentKeys.checklists() });
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

/**
 * Mutation hook to transition a draft document to REQUESTED status.
 * Sends email notification to the client.
 */
export const useSendDocumentRequest = () => {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: ({ transactionId, documentId }: { transactionId: string; documentId: string }) =>
            sendDocumentRequest(transactionId, documentId),
        onSuccess: (_, { transactionId }) => {
            queryClient.invalidateQueries({ queryKey: documentKeys.outstanding() });
            queryClient.invalidateQueries({ queryKey: documentKeys.list(transactionId) });
            queryClient.invalidateQueries({ queryKey: ['documents'] });
            queryClient.invalidateQueries({ queryKey: documentKeys.checklists() });
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

/**
 * Mutation hook to delete a document (typically a draft).
 * Only brokers with EDIT_DOCUMENTS permission can delete documents.
 */
export const useDeleteDocument = () => {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: ({ transactionId, documentId }: { transactionId: string; documentId: string }) =>
            deleteDocument(transactionId, documentId),
        onSuccess: (_, { transactionId }) => {
            queryClient.invalidateQueries({ queryKey: documentKeys.outstanding() });
            queryClient.invalidateQueries({ queryKey: documentKeys.list(transactionId) });
            queryClient.invalidateQueries({ queryKey: ['documents'] });
            queryClient.invalidateQueries({ queryKey: documentKeys.checklists() });
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

/**
 * Mutation hook to share an UPLOAD flow draft document with the client.
 * Transitions from DRAFT to SUBMITTED status.
 */
export const useShareDocumentWithClient = () => {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: ({ transactionId, documentId }: { transactionId: string; documentId: string }) =>
            shareDocumentWithClient(transactionId, documentId),
        onSuccess: (_, { transactionId }) => {
            queryClient.invalidateQueries({ queryKey: documentKeys.outstanding() });
            queryClient.invalidateQueries({ queryKey: documentKeys.list(transactionId) });
            queryClient.invalidateQueries({ queryKey: ['documents'] });
            queryClient.invalidateQueries({ queryKey: documentKeys.checklists() });
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

export const useUpdateChecklistManualState = () => {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: ({
            transactionId,
            itemKey,
            stage,
            checked,
        }: {
            transactionId: string;
            itemKey: string;
            stage: string;
            checked: boolean;
        }) => updateChecklistManualState(transactionId, itemKey, stage, checked),
        onSuccess: (_, { transactionId, stage }) => {
            queryClient.invalidateQueries({ queryKey: documentKeys.checklist(transactionId, stage) });
            queryClient.invalidateQueries({ queryKey: documentKeys.checklists() });
        },
    });
};
