import { useMutation, useQueryClient } from '@tanstack/react-query';
import { documentKeys } from '@/features/documents/api/queries';
import { createDocumentRequest, submitDocument, type CreateDocumentRequestDTO } from './documentsApi.ts';
import { axiosInstance } from '@/shared/api/axiosInstance';


export function useRequestDocument() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: ({ transactionId, data }: { transactionId: string; data: CreateDocumentRequestDTO }) =>
            createDocumentRequest(transactionId, data),
        onSuccess: (_, { transactionId }) => {
            queryClient.invalidateQueries({ queryKey: documentKeys.list(transactionId) });
            queryClient.invalidateQueries({ queryKey: ['documents'] });
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
            queryClient.invalidateQueries({ queryKey: ['documents'] });
        },
    });
}

const reviewDocumentApi = async (
    transactionId: string,
    requestId: string,
    decision: 'APPROVED' | 'NEEDS_REVISION',
    comments?: string
) => {
    const response = await axiosInstance.patch(
        `/transactions/${transactionId}/documents/${requestId}/review`,
        { decision, comments },
        { handleLocally: true }
    );
    return response.data;
};

export const useReviewDocument = () => {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: ({ transactionId, requestId, decision, comments }: {
            transactionId: string;
            requestId: string;
            decision: 'APPROVED' | 'NEEDS_REVISION';
            comments?: string;
        }) => reviewDocumentApi(transactionId, requestId, decision, comments),
        onSuccess: (_, { transactionId }) => {
            queryClient.invalidateQueries({ queryKey: documentKeys.list(transactionId) });
            queryClient.invalidateQueries({ queryKey: ['documents'] });
        },
    });
};

            