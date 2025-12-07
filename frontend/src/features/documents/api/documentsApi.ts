import { axiosInstance } from '@/shared/api/axiosInstance';
import type { DocumentRequest } from '../types';

export const fetchDocuments = async (transactionId: string): Promise<DocumentRequest[]> => {
    const response = await axiosInstance.get<DocumentRequest[]>(`/transactions/${transactionId}/documents`);
    return response.data;
};

export const submitDocument = async (
    transactionId: string,
    requestId: string,
    file: File
): Promise<DocumentRequest> => {
    const formData = new FormData();
    formData.append('file', file);

    const response = await axiosInstance.post<DocumentRequest>(
        `/transactions/${transactionId}/documents/${requestId}/submit`,
        formData,
        {
            headers: {
                'Content-Type': 'multipart/form-data',
            },
        }
    );
    return response.data;
};
