export const DocumentStatusEnum = {
    DRAFT: 'DRAFT',
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
    PROOF_OF_INCOME: 'PROOF_OF_INCOME',
    ID_VERIFICATION: 'ID_VERIFICATION',
    GOVERNMENT_ID_1: 'GOVERNMENT_ID_1',
    GOVERNMENT_ID_2: 'GOVERNMENT_ID_2',
    EMPLOYMENT_LETTER: 'EMPLOYMENT_LETTER',
    PAY_STUBS: 'PAY_STUBS',
    CREDIT_REPORT: 'CREDIT_REPORT',
    BROKERAGE_CONTRACT: 'BROKERAGE_CONTRACT',
    CERTIFICATE_OF_LOCATION: 'CERTIFICATE_OF_LOCATION',
    PROMISE_TO_PURCHASE: 'PROMISE_TO_PURCHASE',
    ACCEPTED_PROMISE_TO_PURCHASE: 'ACCEPTED_PROMISE_TO_PURCHASE',
    ACKNOWLEDGED_SELLERS_DECLARATION: 'ACKNOWLEDGED_SELLERS_DECLARATION',
    SELLERS_DECLARATION: 'SELLERS_DECLARATION',
    INSPECTION_REPORT: 'INSPECTION_REPORT',
    INSURANCE_LETTER: 'INSURANCE_LETTER',
    NOTARY_CONTACT_SHEET: 'NOTARY_CONTACT_SHEET',
    COMPARATIVE_MARKET_ANALYSIS: 'COMPARATIVE_MARKET_ANALYSIS',
    MUNICIPAL_TAX_BILLS: 'MUNICIPAL_TAX_BILLS',
    MORTGAGE_BALANCE_STATEMENT: 'MORTGAGE_BALANCE_STATEMENT',
    SCHOOL_TAX_BILLS: 'SCHOOL_TAX_BILLS',
    CURRENT_DEED_OF_SALE: 'CURRENT_DEED_OF_SALE',
    BANK_STATEMENT: 'BANK_STATEMENT',
    OTHER: 'OTHER',
} as const;
export type DocumentTypeEnum = typeof DocumentTypeEnum[keyof typeof DocumentTypeEnum];

export const DocumentFlowEnum = {
    REQUEST: 'REQUEST',
    UPLOAD: 'UPLOAD',
} as const;
export type DocumentFlowEnum = typeof DocumentFlowEnum[keyof typeof DocumentFlowEnum];

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

export interface DocumentVersion {
    id: number;
    versionId: string;
    uploadedAt: string;
    uploadedBy: UploadedBy;
    storageObject: StorageObject;
}

export interface DocumentCreateDTO {
    docType: DocumentTypeEnum;
    customTitle?: string;
    expectedFrom: DocumentPartyEnum;
    visibleToClient: boolean;
    brokerNotes?: string;
    stage: string;
    conditionIds?: string[];
    dueDate?: Date;
    status?: DocumentStatusEnum;
    flow?: DocumentFlowEnum;
    requiresSignature?: boolean;
}

export interface DocumentUpdateDTO {
    docType?: DocumentTypeEnum;
    customTitle?: string;
    expectedFrom?: DocumentPartyEnum;
    visibleToClient?: boolean;
    brokerNotes?: string;
    stage?: string;
    conditionIds?: string[];
    dueDate?: Date;
    flow?: DocumentFlowEnum;
    requiresSignature?: boolean;
}

export interface Document {
    id: number;
    documentId: string;
    transactionRef: {
        transactionId: string;
        clientId: string;
        side: string;
    };
    docType: DocumentTypeEnum;
    customTitle?: string;
    status: DocumentStatusEnum;
    expectedFrom: DocumentPartyEnum;
    versions: DocumentVersion[];
    brokerNotes?: string;
    lastUpdatedAt?: string;
    visibleToClient: boolean;
    stage: string;
    dueDate?: string;
    flow: DocumentFlowEnum;
    requiresSignature?: boolean;
}
