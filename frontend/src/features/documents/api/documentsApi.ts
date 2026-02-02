import { axiosInstance } from '@/shared/api/axiosInstance';
import type { Document, DocumentTypeEnum, DocumentPartyEnum } from '../types';

export interface CreateDocumentDTO {
    docType: DocumentTypeEnum;
    customTitle?: string;
    expectedFrom: DocumentPartyEnum;
    visibleToClient?: boolean;
    brokerNotes?: string;
    stage: string;
    conditionIds?: string[];
    dueDate?: Date;
    /** Optional status: 'DRAFT' or 'REQUESTED'. Defaults to 'REQUESTED' if not provided. */
    status?: 'DRAFT' | 'REQUESTED';
}

export interface UpdateDocumentDTO {
    docType?: DocumentTypeEnum;
    customTitle?: string;
    expectedFrom?: DocumentPartyEnum;
    visibleToClient?: boolean;
    brokerNotes?: string;
    dueDate?: Date;
}

export interface OutstandingDocumentDTO {
    id: string;
    title: string;
    transactionAddress: string;
    clientName: string;
    clientEmail: string;
    dueDate: string | null;
    daysOutstanding: number | null;
    status: string;
}

export const fetchDocuments = async (transactionId: string): Promise<Document[]> => {
    const response = await axiosInstance.get<Document[]>(`/transactions/${transactionId}/documents`);
    return response.data;
};

export const fetchAllDocuments = async (): Promise<Document[]> => {
    const response = await axiosInstance.get<Document[]>('/documents');
    return response.data;
};

export const fetchOutstandingDocuments = async (): Promise<OutstandingDocumentDTO[]> => {
    const response = await axiosInstance.get<OutstandingDocumentDTO[]>('/documents/outstanding');
    return response.data;
};

export const createDocument = async (
    transactionId: string,
    data: CreateDocumentDTO
): Promise<Document> => {
    const response = await axiosInstance.post<Document>(
        `/transactions/${transactionId}/documents`,
        data,
        { handleLocally: true }
    );
    return response.data;
};

export const getDocument = async (
    transactionId: string,
    documentId: string
): Promise<Document> => {
    const response = await axiosInstance.get<Document>(
        `/transactions/${transactionId}/documents/${documentId}`
    );
    return response.data;
};

export const updateDocument = async (
    transactionId: string,
    documentId: string,
    data: UpdateDocumentDTO
): Promise<Document> => {
    const response = await axiosInstance.put<Document>(
        `/transactions/${transactionId}/documents/${documentId}`,
        data,
        { handleLocally: true }
    );
    return response.data;
};

export const deleteDocument = async (
    transactionId: string,
    documentId: string
): Promise<void> => {
    await axiosInstance.delete(`/transactions/${transactionId}/documents/${documentId}`, { handleLocally: true });
};

export const submitDocument = async (
    transactionId: string,
    documentId: string,
    file: File
): Promise<Document> => {
    const formData = new FormData();
    formData.append('file', file);

    const response = await axiosInstance.post<Document>(
        `/transactions/${transactionId}/documents/${documentId}/submit`,
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
    documentId: string,
    versionId: string
): Promise<string> => {
    const response = await axiosInstance.get<{ url: string }>(
        `/transactions/${transactionId}/documents/${documentId}/versions/${versionId}/download`
    );
    return response.data.url;
};


export const reviewDocument = async (
    transactionId: string,
    documentId: string,
    decision: 'APPROVED' | 'NEEDS_REVISION',
    comments?: string
): Promise<Document> => {
    const response = await axiosInstance.patch<Document>(
        `/transactions/${transactionId}/documents/${documentId}/review`,
        { decision, comments },
        { handleLocally: true }
    );
    return response.data;
};

export const sendDocumentReminder = async (documentId: string): Promise<void> => {
    await axiosInstance.post(`/documents/${documentId}/remind`);
};

/**
 * Transitions a draft document to REQUESTED status.
 * Sends email notification to the client.
 */
export const sendDocumentRequest = async (
    transactionId: string,
    documentId: string
): Promise<Document> => {
    const response = await axiosInstance.post<Document>(
        `/transactions/${transactionId}/documents/${documentId}/send`,
        {},
        { handleLocally: true }
    );
    return response.data;
};
