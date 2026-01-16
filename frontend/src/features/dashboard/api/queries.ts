import { useQuery } from '@tanstack/react-query';
import { axiosInstance } from '@/shared/api/axiosInstance';

export interface ExpiringOffer {
    offerId: string;
    transactionId: string;
    propertyAddress: string;
    clientName: string;
    offerAmount: number;
    expiryDate: string;
    daysUntilExpiry: number;
    offerType: 'BUY_SIDE' | 'SELL_SIDE';
    status: string;
}

export interface PendingDocument {
    requestId: string;
    transactionId: string;
    clientName: string;
    documentType: string;
    customTitle: string | null;
    submittedAt: string | null;
    propertyAddress: string;
}

/**
 * Transaction context information embedded in timeline entries.
 */
export interface TransactionInfo {
    clientName?: string;
    address?: string;
    actorName?: string;
    stage?: string;
    previousStage?: string;
    newStage?: string;
    buyerName?: string;
    offerAmount?: number;
    offerStatus?: string;
    previousOfferStatus?: string;
    conditionType?: string;
    conditionCustomTitle?: string;
    conditionDescription?: string;
    conditionDeadline?: string;
    conditionPreviousStatus?: string;
    conditionNewStatus?: string;
}

export interface RecentActivity {
    activityId: string;
    transactionId: string;
    type: string;
    title: string | null;
    note: string | null;
    occurredAt: string;
    actorName: string | null;
    propertyAddress: string;
    // Enriched transaction context
    clientName: string;
    side: 'BUY_SIDE' | 'SELL_SIDE' | '';
    currentStage: string;
    // Document-specific fields
    docType: string | null;
    status: string | null;
    // Full transaction info for event-specific details
    transactionInfo: TransactionInfo | null;
    // Whether this activity has been seen by the broker
    seen: boolean;
}

export interface RecentActivityPage {
    content: RecentActivity[];
    page: number;
    size: number;
    totalElements: number;
    totalPages: number;
    first: boolean;
    last: boolean;
}

export interface PinnedTransaction {
    transactionId: string;
    clientName: string;
    propertyAddress: string;
    side: 'BUY_SIDE' | 'SELL_SIDE';
    status: string;
    currentStage: string;
}

export const dashboardKeys = {
    all: ['dashboard'] as const,
    brokerStats: () => [...dashboardKeys.all, 'broker'] as const,
    expiringOffers: () => [...dashboardKeys.all, 'expiring-offers'] as const,
    pendingDocuments: () => [...dashboardKeys.all, 'pending-documents'] as const,
    recentActivity: (page: number, size: number) => [...dashboardKeys.all, 'recent-activity', page, size] as const,
    pinnedTransactions: () => [...dashboardKeys.all, 'pinned-transactions'] as const,
};

export function useExpiringOffers() {
    return useQuery({
        queryKey: dashboardKeys.expiringOffers(),
        queryFn: async () => {
            const res = await axiosInstance.get<ExpiringOffer[]>('/api/v1/dashboard/broker/expiring-offers');
            return res.data;
        },
    });
}

export function usePendingDocuments() {
    return useQuery({
        queryKey: dashboardKeys.pendingDocuments(),
        queryFn: async () => {
            const res = await axiosInstance.get<PendingDocument[]>('/api/v1/dashboard/broker/pending-documents');
            return res.data;
        },
    });
}

export function useRecentActivity(page: number = 0, size: number = 10) {
    return useQuery({
        queryKey: dashboardKeys.recentActivity(page, size),
        queryFn: async () => {
            const res = await axiosInstance.get<RecentActivityPage>('/api/v1/dashboard/broker/recent-activity', {
                params: { page, size }
            });
            return res.data;
        },
    });
}

export function usePinnedTransactions() {
    return useQuery({
        queryKey: dashboardKeys.pinnedTransactions(),
        queryFn: async () => {
            const res = await axiosInstance.get<PinnedTransaction[]>('/api/v1/dashboard/broker/pinned-transactions');
            return res.data;
        },
    });
}

