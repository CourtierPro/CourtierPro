import { axiosInstance } from '@/shared/api/axiosInstance';
import type { DocumentRequest, DocumentTypeEnum, DocumentPartyEnum } from '../types';

export interface CreateDocumentRequestDTO {
    docType: DocumentTypeEnum;
    customTitle?: string;
    expectedFrom: DocumentPartyEnum;
    visibleToClient?: boolean;
    brokerNotes?: string;
}

export interface UpdateDocumentRequestDTO {
    docType?: DocumentTypeEnum;
    customTitle?: string;
    expectedFrom?: DocumentPartyEnum;
    visibleToClient?: boolean;
    brokerNotes?: string;
}

export const fetchDocuments = async (transactionId: string): Promise<DocumentRequest[]> => {
    const response = await axiosInstance.get<DocumentRequest[]>(`/transactions/${transactionId}/documents`);
    return response.data;
};

export const createDocumentRequest = async (
    transactionId: string,
    data: CreateDocumentRequestDTO
): Promise<DocumentRequest> => {
    const response = await axiosInstance.post<DocumentRequest>(
        `/transactions/${transactionId}/documents`,
        data
    );
    return response.data;
};

export const getDocumentRequest = async (
    transactionId: string,
    requestId: string
): Promise<DocumentRequest> => {
    const response = await axiosInstance.get<DocumentRequest>(
        `/transactions/${transactionId}/documents/${requestId}`
    );
    return response.data;
};

export const updateDocumentRequest = async (
    transactionId: string,
    requestId: string,
    data: UpdateDocumentRequestDTO
): Promise<DocumentRequest> => {
    const response = await axiosInstance.put<DocumentRequest>(
        `/transactions/${transactionId}/documents/${requestId}`,
        data
    );
    return response.data;
};

export const deleteDocumentRequest = async (
    transactionId: string,
    requestId: string
): Promise<void> => {
    await axiosInstance.delete(`/transactions/${transactionId}/documents/${requestId}`);
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
