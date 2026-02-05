import { axiosInstance } from '@/shared/api/axiosInstance';
import type { Document, DocumentTypeEnum, DocumentPartyEnum, DocumentFlowEnum } from '../types';

export interface CreateDocumentDTO {
    docType: DocumentTypeEnum;
    customTitle?: string;
    expectedFrom: DocumentPartyEnum;
    visibleToClient?: boolean;
    brokerNotes?: string;
    stage: string;
    conditionIds?: string[];
    dueDate?: Date;
    /** Optional status: 'DRAFT', 'REQUESTED', or 'SUBMITTED'. Defaults to 'REQUESTED' if not provided. */
    status?: 'DRAFT' | 'REQUESTED' | 'SUBMITTED';
    /** Optional flow type: 'REQUEST' or 'UPLOAD'. Defaults to 'REQUEST' if not provided. */
    flow?: DocumentFlowEnum;
    /** Whether this document requires the client's signature */
    requiresSignature?: boolean;
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

/**
 * Uploads a file to a document without changing its status.
 * Used for attaching files to draft documents.
 */
export const uploadFileToDocument = async (
    transactionId: string,
    documentId: string,
    file: File
): Promise<Document> => {
    const formData = new FormData();
    formData.append('file', file);

    const response = await axiosInstance.post<Document>(
        `/transactions/${transactionId}/documents/${documentId}/upload`,
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

/**
 * Shares an UPLOAD flow draft document with the client.
 * Transitions from DRAFT to SUBMITTED status.
 */
export const shareDocumentWithClient = async (
    transactionId: string,
    documentId: string
): Promise<Document> => {
    const response = await axiosInstance.post<Document>(
        `/transactions/${transactionId}/documents/${documentId}/share`,
        {},
        { handleLocally: true }
    );
    return response.data;
};
