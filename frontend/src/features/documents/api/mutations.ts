import { useMutation, useQueryClient } from '@tanstack/react-query';
import { documentKeys, type Document } from '@/features/documents/api/queries';

export interface RequestDocumentDTO {
    title: string;
    recipientEmail: string;
    message: string;
    transactionId: string;
}

export function useRequestDocument() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async (data: RequestDocumentDTO) => {
            // In a real app: const res = await axiosInstance.post('/documents/request', data);
            await new Promise((resolve) => setTimeout(resolve, 1000));
            return { id: 'temp-id', ...data, status: 'pending', uploadedAt: new Date().toISOString() } as unknown as Document;
        },
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: documentKeys.lists() });
        },
    });
}
