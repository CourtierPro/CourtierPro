import axiosInstance from "@/shared/api/axiosInstance";

// Types
export type ResourceType = "TRANSACTION" | "DOCUMENT_REQUEST";

export interface ResourceItem {
    id: string;
    summary: string;
    createdAt: string | null;
    deletedAt: string | null;
    deletedBy: string | null;
    deleted: boolean;  // Jackson serializes 'isDeleted' as 'deleted'

    // Transaction details
    clientId: string | null;
    clientEmail: string | null;
    brokerId: string | null;
    brokerEmail: string | null;
    status: string | null;
    side: string | null;
    address: string | null;

    // Document request details
    transactionId: string | null;
    docType: string | null;
    submittedDocCount: number;
}

export interface ResourceListResponse {
    resourceType: string;
    totalCount: number;
    deletedCount: number;
    items: ResourceItem[];
}

export interface LinkedResource {
    type: string;
    id: string | null;
    summary: string;
}

export interface S3FileToDelete {
    fileName: string;
    mimeType: string;
    sizeBytes: number;
}

export interface DeletionPreviewResponse {
    resourceId: string;
    resourceType: string;
    resourceSummary: string;
    linkedResources: LinkedResource[];
    s3FilesToDelete: S3FileToDelete[];
}

export interface AuditLogEntry {
    id: number;
    action: "DELETE" | "RESTORE";
    timestamp: string;
    adminId: string;
    adminEmail: string;
    resourceType: string;
    resourceId: string;
    resourceSnapshot: string;
    cascadedDeletions: string;
}

// API functions
export async function listResources(
    type: ResourceType,
    includeDeleted = false
): Promise<ResourceListResponse> {
    const res = await axiosInstance.get<ResourceListResponse>(
        `/api/admin/resources/${type}`,
        { params: { includeDeleted } }
    );
    return res.data;
}

export async function previewDeletion(
    type: ResourceType,
    resourceId: string
): Promise<DeletionPreviewResponse> {
    const res = await axiosInstance.get<DeletionPreviewResponse>(
        `/api/admin/resources/${type}/${resourceId}/preview`
    );
    return res.data;
}

export async function deleteResource(
    type: ResourceType,
    resourceId: string
): Promise<void> {
    await axiosInstance.delete(`/api/admin/resources/${type}/${resourceId}`, {
        params: { confirmed: true },
    });
}

export async function restoreResource(
    type: ResourceType,
    resourceId: string
): Promise<void> {
    await axiosInstance.post(
        `/api/admin/resources/${type}/${resourceId}/restore`
    );
}

export async function getAuditHistory(): Promise<AuditLogEntry[]> {
    const res = await axiosInstance.get<AuditLogEntry[]>(
        "/api/admin/resources/audit-history"
    );
    return res.data;
}
