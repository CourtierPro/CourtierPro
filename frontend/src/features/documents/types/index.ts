export const DocumentStatusEnum = {
    REQUESTED: 'REQUESTED',
    SUBMITTED: 'SUBMITTED',
    APPROVED: 'APPROVED',
    NEEDS_REVISION: 'NEEDS_REVISION',
    REJECTED: 'REJECTED',
} as const;
export type DocumentStatusEnum = typeof DocumentStatusEnum[keyof typeof DocumentStatusEnum];

export const DocumentPartyEnum = {
    BUYER: 'BUYER',
    SELLER: 'SELLER',
    BROKER: 'BROKER',
    LENDER: 'LENDER',
    NOTARY: 'NOTARY',
    INSPECTOR: 'INSPECTOR',
    OTHER: 'OTHER',
} as const;
export type DocumentPartyEnum = typeof DocumentPartyEnum[keyof typeof DocumentPartyEnum];

export const DocumentTypeEnum = {
    MORTGAGE_PRE_APPROVAL: 'MORTGAGE_PRE_APPROVAL',
    MORTGAGE_APPROVAL: 'MORTGAGE_APPROVAL',
    PROOF_OF_FUNDS: 'PROOF_OF_FUNDS',
    ID_VERIFICATION: 'ID_VERIFICATION',
    EMPLOYMENT_LETTER: 'EMPLOYMENT_LETTER',
    PAY_STUBS: 'PAY_STUBS',
    CREDIT_REPORT: 'CREDIT_REPORT',
    CERTIFICATE_OF_LOCATION: 'CERTIFICATE_OF_LOCATION',
    PROMISE_TO_PURCHASE: 'PROMISE_TO_PURCHASE',
    INSPECTION_REPORT: 'INSPECTION_REPORT',
    INSURANCE_LETTER: 'INSURANCE_LETTER',
    BANK_STATEMENT: 'BANK_STATEMENT',
    OTHER: 'OTHER',
} as const;
export type DocumentTypeEnum = typeof DocumentTypeEnum[keyof typeof DocumentTypeEnum];

export interface StorageObject {
    s3Key: string;
    fileName: string;
    mimeType: string;
    sizeBytes: number;
}

export interface UploadedBy {
    uploaderType: string;
    party: DocumentPartyEnum;
    uploaderId: string;
    externalName?: string;
}

export interface SubmittedDocument {
    id: number;
    documentId: string;
    uploadedAt: string;
    uploadedBy: UploadedBy;
    storageObject: StorageObject;
}

export interface DocumentRequest {
    id: number;
    requestId: string;
    transactionRef: {
        transactionId: string;
        clientId: string;
        side: string;
    };
    docType: DocumentTypeEnum;
    customTitle?: string;
    status: DocumentStatusEnum;
    expectedFrom: DocumentPartyEnum;
    submittedDocuments: SubmittedDocument[];
    brokerNotes?: string;
    lastUpdatedAt?: string;
    visibleToClient: boolean;
    stage: string; // Added to match backend and fix filtering
}
