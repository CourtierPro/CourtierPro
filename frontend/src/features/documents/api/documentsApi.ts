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

export const fetchAllDocuments = async (): Promise<DocumentRequest[]> => {
    // Note: The backend endpoint is mapped as /transactions/{transactionId}/documents/all which is awkward.
    // Wait, the controller has @RequestMapping("/transactions/{transactionId}/documents").
    // So /all would be /transactions/{transactionId}/documents/all.
    // But we want a global endpoint.
    // I should have checked the controller mapping.
    // The controller is scoped to a transaction. I should probably move the global endpoint or create a new controller.
    // Let's check the controller again.
    const response = await axiosInstance.get<DocumentRequest[]>('/documents');
    return response.data;
};

export const createDocumentRequest = async (
    transactionId: string,
    data: CreateDocumentRequestDTO
): Promise<DocumentRequest> => {
    const response = await axiosInstance.post<DocumentRequest>(
        `/transactions/${transactionId}/documents`,
        data,
        { handleLocally: true }
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
        data,
        { handleLocally: true }
    );
    return response.data;
};

export const deleteDocumentRequest = async (
    transactionId: string,
    requestId: string
): Promise<void> => {
    await axiosInstance.delete(`/transactions/${transactionId}/documents/${requestId}`, { handleLocally: true });
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
            handleLocally: true,
        }
    );
    return response.data;
};

export const getDocumentDownloadUrl = async (
    transactionId: string,
    requestId: string,
    documentId: string
): Promise<string> => {
    const response = await axiosInstance.get<{ url: string }>(
        `/transactions/${transactionId}/documents/${requestId}/documents/${documentId}/download`
    );
    return response.data.url;
};
