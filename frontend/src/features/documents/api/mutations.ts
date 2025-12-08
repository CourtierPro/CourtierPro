import { useMutation, useQueryClient } from '@tanstack/react-query';
import { documentKeys } from '@/features/documents/api/queries';
import { createDocumentRequest, submitDocument, type CreateDocumentRequestDTO } from './documentsApi';

export function useRequestDocument() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: ({ transactionId, data }: { transactionId: string; data: CreateDocumentRequestDTO }) =>
            createDocumentRequest(transactionId, data),
        onSuccess: (_, { transactionId }) => {
            queryClient.invalidateQueries({ queryKey: documentKeys.list(transactionId) });
        },
    });
}

export function useSubmitDocument() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: ({ transactionId, requestId, file }: { transactionId: string; requestId: string; file: File }) =>
            submitDocument(transactionId, requestId, file),
        onSuccess: (_, { transactionId }) => {
            queryClient.invalidateQueries({ queryKey: documentKeys.list(transactionId) });
        },
    });
}
