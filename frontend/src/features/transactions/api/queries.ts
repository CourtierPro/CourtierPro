import { useQuery } from '@tanstack/react-query';
import axiosInstance from '@/shared/api/axiosInstance';
import type { TimelineEntryDTO, TransactionParticipant } from '@/shared/api/types';

// Re-export a local type alias expected by components
export type TimelineEntry = TimelineEntryDTO;
export type Participant = TransactionParticipant;

export const transactionKeys = {
    all: ['transactions'] as const,
    lists: () => [...transactionKeys.all, 'list'] as const,
    details: () => [...transactionKeys.all, 'detail'] as const,
    detail: (id: string) => [...transactionKeys.details(), id] as const,
    participants: (id: string) => [...transactionKeys.detail(id), 'participants'] as const,
    client: (clientId: string) => [...transactionKeys.all, 'client', clientId] as const,
    pinned: () => [...transactionKeys.all, 'pinned'] as const,
};

export interface Transaction {
    transactionId: string;
    clientId: string;
    clientName: string;
    brokerName?: string;
    propertyAddress: {
        street: string;
        city: string;
        province: string;
        postalCode: string;
    };
    centrisNumber?: string;
    side: 'BUY_SIDE' | 'SELL_SIDE';
    currentStage: string | number;
    totalStages: number;
    status: 'ACTIVE' | 'CLOSED_SUCCESSFULLY' | 'TERMINATED_EARLY';
    openedAt?: string;
    openedDate?: string;
    lastUpdated?: string;
    notes?: string;
    brokerId?: string;
    archived?: boolean;
    archivedAt?: string;
    houseVisitCount?: number;
    totalShowings?: number;
    totalVisitors?: number;
}

export function usePinnedTransactionIds() {
    return useQuery({
        queryKey: transactionKeys.pinned(),
        queryFn: async () => {
            const res = await axiosInstance.get<string[]>('/transactions/pinned');
            return new Set(res.data);
        },
    });
}

export function useArchivedTransactions() {
    return useQuery({
        queryKey: [...transactionKeys.all, 'archived'] as const,
        queryFn: async () => {
            const res = await axiosInstance.get<Transaction[]>('/transactions/archived');
            return res.data;
        },
    });
}

export function useTransactions(filters?: { status?: string; stage?: string; side?: string }, options?: { enabled?: boolean }) {
    return useQuery({
        queryKey: [...transactionKeys.lists(), filters],
        queryFn: async () => {
            const params = new URLSearchParams();
            if (filters?.status && filters.status !== 'all') params.append('status', filters.status);
            if (filters?.stage && filters.stage !== 'all') params.append('stage', filters.stage);
            if (filters?.side && filters.side !== 'all') {
                // Map frontend filter values to backend enum values
                const sideValue = filters.side === 'buy' ? 'BUY_SIDE' : filters.side === 'sell' ? 'SELL_SIDE' : filters.side;
                params.append('side', sideValue);
            }

            const res = await axiosInstance.get<Transaction[]>(`/transactions?${params.toString()}`);
            return res.data;
        },
        enabled: options?.enabled,
    });
}

export function useTransaction(id: string | undefined) {
    return useQuery({
        queryKey: transactionKeys.detail(id!),
        queryFn: async () => {
            const res = await axiosInstance.get<Transaction>(`/transactions/${id}`);
            return res.data;
        },
        enabled: !!id,
    });
}

export function useClientTransactions(clientId: string) {
    return useQuery({
        queryKey: transactionKeys.client(clientId),
        queryFn: async () => {
            const res = await axiosInstance.get<Transaction[]>(`/clients/${encodeURIComponent(clientId)}/transactions`);
            return res.data;
        },
        enabled: !!clientId,
    });
}

/**
 * Hook for brokers to fetch transactions for a specific client.
 * Uses the broker-specific endpoint that filters to transactions where the logged-in user is the broker.
 */
export function useBrokerClientTransactions(clientId: string) {
    return useQuery({
        queryKey: [...transactionKeys.client(clientId), 'broker'] as const,
        queryFn: async () => {
            const res = await axiosInstance.get<Transaction[]>(`/api/broker/clients/${encodeURIComponent(clientId)}/transactions`);
            return res.data;
        },
        enabled: !!clientId,
    });
}

/**
 * Hook for brokers to fetch ALL transactions for a client (across all brokers).
 * Returns broker names with each transaction for display in client info modal.
 */
export function useAllClientTransactions(clientId: string) {
    return useQuery({
        queryKey: [...transactionKeys.client(clientId), 'all'] as const,
        queryFn: async () => {
            const res = await axiosInstance.get<Transaction[]>(`/api/broker/clients/${encodeURIComponent(clientId)}/all-transactions`);
            return res.data;
        },
        enabled: !!clientId,
    });
}

export function useTransactionTimeline(transactionId: string) {
    return useQuery({
        queryKey: [
            ...transactionKeys.detail(transactionId),
            'timeline',
        ],
        queryFn: async () => {
            const res = await axiosInstance.get<TimelineEntry[]>(`/transactions/${transactionId}/timeline`);
            return res.data;
        },
        enabled: !!transactionId,
    });
}

export function useClientTransactionTimeline(transactionId: string) {
    return useQuery({
        queryKey: [
            ...transactionKeys.detail(transactionId),
            'timeline',
            'client'
        ],
        queryFn: async () => {
            const res = await axiosInstance.get<TimelineEntryDTO[]>(`/transactions/${transactionId}/timeline/client`);
            return res.data;
        },
        enabled: !!transactionId,
    });
}

export function useTransactionParticipants(transactionId: string) {
    return useQuery({
        queryKey: transactionKeys.participants(transactionId),
        queryFn: async () => {
            const res = await axiosInstance.get<Participant[]>(`/transactions/${transactionId}/participants`);
            return res.data;
        },
        enabled: !!transactionId,
    });
}

export interface BrokerUser {
    id: string;
    email: string;
    firstName: string;
    lastName: string;
    role: string;
}

export function useBrokers() {
    return useQuery({
        queryKey: ['brokers'],
        queryFn: async () => {
            const res = await axiosInstance.get<BrokerUser[]>('/api/broker/colleagues');
            return res.data;
        },
    });
}

// ==================== PROPERTY QUERIES ====================

import type { Property } from '@/shared/api/types';

export const propertyKeys = {
    all: (transactionId: string) => [...transactionKeys.detail(transactionId), 'properties'] as const,
    detail: (transactionId: string, propertyId: string) => [...propertyKeys.all(transactionId), propertyId] as const,
};

export function useTransactionProperties(transactionId: string) {
    return useQuery({
        queryKey: propertyKeys.all(transactionId),
        queryFn: async () => {
            // Backend now handles both broker and client access with role-based response
            const res = await axiosInstance.get<Property[]>(`/transactions/${transactionId}/properties`);
            return res.data;
        },
        enabled: !!transactionId,
    });
}

export function useProperty(transactionId: string, propertyId: string) {
    return useQuery({
        queryKey: propertyKeys.detail(transactionId, propertyId),
        queryFn: async () => {
            const res = await axiosInstance.get<Property>(`/transactions/${transactionId}/properties/${propertyId}`);
            return res.data;
        },
        enabled: !!transactionId && !!propertyId,
    });
}

// ==================== OFFER QUERIES ====================

import type { Offer } from '@/shared/api/types';

export const offerKeys = {
    all: (transactionId: string) => [...transactionKeys.detail(transactionId), 'offers'] as const,
    detail: (transactionId: string, offerId: string) => [...offerKeys.all(transactionId), offerId] as const,
};

export function useTransactionOffers(transactionId: string, enabled: boolean = true, clientId?: string) {
    return useQuery({
        queryKey: clientId
            ? [...offerKeys.all(transactionId), 'client', clientId] as const
            : offerKeys.all(transactionId),
        queryFn: async () => {
            // Use client-specific endpoint if clientId is provided
            const url = clientId
                ? `/clients/${clientId}/transactions/${transactionId}/offers`
                : `/transactions/${transactionId}/offers`;
            const res = await axiosInstance.get<Offer[]>(url);
            return res.data;
        },
        enabled: enabled && !!transactionId,
    });
}

export function useOffer(transactionId: string, offerId: string) {
    return useQuery({
        queryKey: offerKeys.detail(transactionId, offerId),
        queryFn: async () => {
            const res = await axiosInstance.get<Offer>(`/transactions/${transactionId}/offers/${offerId}`);
            return res.data;
        },
        enabled: !!transactionId && !!offerId,
    });
}

// ==================== CONDITION QUERIES ====================

import type { Condition } from '@/shared/api/types';

export const conditionKeys = {
    all: (transactionId: string) => [...transactionKeys.detail(transactionId), 'conditions'] as const,
    detail: (transactionId: string, conditionId: string) => [...conditionKeys.all(transactionId), conditionId] as const,
};

export function useTransactionConditions(transactionId: string) {
    return useQuery({
        queryKey: conditionKeys.all(transactionId),
        queryFn: async () => {
            const res = await axiosInstance.get<Condition[]>(`/transactions/${transactionId}/conditions`);
            return res.data;
        },
        enabled: !!transactionId,
    });
}

// ==================== PROPERTY OFFER QUERIES ====================

import type { PropertyOffer, OfferDocument, OfferRevision } from '@/shared/api/types';

export const propertyOfferKeys = {
    all: (propertyId: string) => ['propertyOffers', propertyId] as const,
    detail: (propertyId: string, propertyOfferId: string) => [...propertyOfferKeys.all(propertyId), propertyOfferId] as const,
    documents: (propertyOfferId: string) => ['propertyOfferDocuments', propertyOfferId] as const,
};

export const offerDocumentKeys = {
    all: (offerId: string) => ['offerDocuments', offerId] as const,
};

export const offerRevisionKeys = {
    all: (offerId: string) => ['offerRevisions', offerId] as const,
};

export function usePropertyOffers(propertyId: string) {
    return useQuery({
        queryKey: propertyOfferKeys.all(propertyId),
        queryFn: async () => {
            const res = await axiosInstance.get<PropertyOffer[]>(`/transactions/properties/${propertyId}/offers`);
            return res.data;
        },
        enabled: !!propertyId,
    });
}

export function usePropertyOfferDocuments(propertyOfferId: string) {
    return useQuery({
        queryKey: propertyOfferKeys.documents(propertyOfferId),
        queryFn: async () => {
            // Uses a placeholder propertyId since the endpoint requires it but doesn't use it for retrieval
            const res = await axiosInstance.get<OfferDocument[]>(`/transactions/properties/placeholder/offers/${propertyOfferId}/documents`);
            return res.data;
        },
        enabled: !!propertyOfferId,
    });
}

export function useOfferDocuments(transactionId: string, offerId: string, clientId?: string) {
    return useQuery({
        queryKey: clientId
            ? [...offerDocumentKeys.all(offerId), 'client', clientId] as const
            : offerDocumentKeys.all(offerId),
        queryFn: async () => {
            // Use client-specific endpoint if clientId is provided
            const url = clientId
                ? `/clients/${clientId}/transactions/${transactionId}/offers/${offerId}/documents`
                : `/transactions/${transactionId}/offers/${offerId}/documents`;
            const res = await axiosInstance.get<OfferDocument[]>(url);
            return res.data;
        },
        enabled: !!transactionId && !!offerId,
    });
}

export function useOfferRevisions(transactionId: string, offerId: string) {
    return useQuery({
        queryKey: offerRevisionKeys.all(offerId),
        queryFn: async () => {
            const res = await axiosInstance.get<OfferRevision[]>(`/transactions/${transactionId}/offers/${offerId}/revisions`);
            return res.data;
        },
        enabled: !!transactionId && !!offerId,
    });
}

// ==================== UNIFIED DOCUMENTS QUERY ====================

import type { UnifiedDocument } from '@/shared/api/types';

export const allDocumentKeys = {
    all: (transactionId: string) => ['allTransactionDocuments', transactionId] as const,
};

export function useAllTransactionDocuments(transactionId: string, clientId?: string) {
    return useQuery({
        queryKey: clientId
            ? [...allDocumentKeys.all(transactionId), 'client', clientId] as const
            : allDocumentKeys.all(transactionId),
        queryFn: async () => {
            // Use same endpoint for both broker and client - access control is server-side
            const res = await axiosInstance.get<UnifiedDocument[]>(`/transactions/${transactionId}/all-documents`);
            return res.data;
        },
        enabled: !!transactionId,
    });
}

// ==================== SEARCH CRITERIA QUERIES ====================

import type { SearchCriteria } from '@/shared/api/types';

export const searchCriteriaKeys = {
    byTransaction: (transactionId: string) => [...transactionKeys.detail(transactionId), 'searchCriteria'] as const,
};

export function useSearchCriteria(transactionId: string) {
    return useQuery({
        queryKey: searchCriteriaKeys.byTransaction(transactionId),
        queryFn: async () => {
            const res = await axiosInstance.get<SearchCriteria | null>(`/transactions/${transactionId}/search-criteria`);
            return res.data;
        },
        enabled: !!transactionId,
    });
}

// ==================== VISITOR QUERIES ====================

import type { Visitor } from '@/shared/api/types';

export const visitorKeys = {
    byTransaction: (transactionId: string) => [...transactionKeys.detail(transactionId), 'visitors'] as const,
};

export function useTransactionVisitors(transactionId: string) {
    return useQuery({
        queryKey: visitorKeys.byTransaction(transactionId),
        queryFn: async () => {
            const res = await axiosInstance.get<Visitor[]>(`/transactions/${transactionId}/visitors`);
            return res.data;
        },
        enabled: !!transactionId,
    });
}
