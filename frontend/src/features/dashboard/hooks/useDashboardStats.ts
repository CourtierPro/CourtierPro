import { useQuery } from '@tanstack/react-query';
import { useClientTransactions } from '@/features/transactions/api/queries';
import { fetchAllDocuments } from '@/features/documents/api/documentsApi';
import type { DocumentRequest } from '@/features/documents/types';
import { documentKeys } from '@/features/documents/api/queries';
import { axiosInstance } from '@/shared/api/axiosInstance';

export interface ClientDashboardStats {
    activeTransactions: number;
    documentsNeeded: number;
    documentsSubmitted: number;
}

export interface BrokerDashboardStats {
    activeTransactions: number;
    activeClients: number;
    totalCommission: number;
    pendingDocumentReviews: number;
    expiringOffersCount: number;
    approachingConditionsCount: number;
    outstandingDocumentsCount: number;
}

export interface AdminDashboardStats {
    totalUsers: number;
    activeBrokers: number;
    clientCount: number;
    activeTransactions: number;
    newUsers: number;
    failedLogins: number;
    systemHealth: string;
}

export interface RecentActionsResponse {
    recentLogins: unknown[];
    recentDeletions: unknown[];
}

import { useState, useMemo } from 'react';

export function useClientDashboardStats(clientId: string) {
    const { data: transactions = [], isLoading: isLoadingTransactions, error: errorTransactions } = useClientTransactions(clientId);
    const { data: allDocuments = [], isLoading: isLoadingDocuments, error: errorDocuments } = useQuery<DocumentRequest[]>({
        queryKey: documentKeys.all,
        queryFn: fetchAllDocuments,
    });
    const clientDocuments = allDocuments.filter(d => d.transactionRef?.clientId === clientId);

    const [selectedTransactionId, setSelectedTransactionId] = useState<string | null>(null);
    const selectedTransaction = useMemo(() => {
        if (!transactions.length) return null;
        if (selectedTransactionId) return transactions.find(t => t.transactionId === selectedTransactionId) || transactions[0];
        // Par défaut, la première active, sinon la première
        return transactions.find(t => t.status === 'ACTIVE') || transactions[0];
    }, [transactions, selectedTransactionId]);

    const selectedDocuments = useMemo(() => {
        if (!selectedTransaction) return [];
        return clientDocuments.filter(d => d.transactionRef?.transactionId === selectedTransaction.transactionId);
    }, [clientDocuments, selectedTransaction]);

    const activeTransactions = transactions.filter(t => t.status === 'ACTIVE');
    const documentsNeededGlobal = clientDocuments.filter(d => d.status === 'REQUESTED');
    // Submitted = SUBMITTED or APPROVED (NEEDS_REVISION is excluded until resubmitted)
    const documentsSubmittedGlobal = clientDocuments.filter(d => d.status === 'SUBMITTED' || d.status === 'APPROVED');

    const documentsNeeded = selectedDocuments.filter(d => d.status === 'REQUESTED');
    const documentsSubmitted = selectedDocuments.filter(d => d.status === 'SUBMITTED' || d.status === 'APPROVED');
    // Placeholder offers count (client dashboard currently derives offer totals from per-transaction fetches)
    const offersReceived = 0;

    return {
        transactions,
        selectedTransaction,
        setSelectedTransactionId,
        selectedTransactionId,
        clientDocuments,
        kpis: {
            global: {
                activeTransactions: activeTransactions.length,
                documentsNeeded: documentsNeededGlobal.length,
                documentsSubmitted: documentsSubmittedGlobal.length,
                offersReceived,
            },
            selected: {
                documentsNeeded: documentsNeeded.length,
                documentsSubmitted: documentsSubmitted.length,
            },
        },
        isLoading: isLoadingTransactions || isLoadingDocuments,
        error: errorTransactions || errorDocuments,
    };
}

export function useBrokerDashboardStats() {
    return useQuery({
        queryKey: ['dashboard', 'broker'],
        queryFn: async () => {
            const res = await axiosInstance.get<BrokerDashboardStats>('/api/v1/dashboard/broker');
            return res.data;
        },
    });
}

export function useAdminDashboardStats() {
    return useQuery({
        queryKey: ['dashboard', 'admin'],
        queryFn: async () => {
            const res = await axiosInstance.get<AdminDashboardStats>('/api/v1/dashboard/admin');
            return res.data;
        },
    });
}

export function useAdminRecentActions() {
    return useQuery({
        queryKey: ['dashboard', 'admin', 'recent-actions'],
        queryFn: async () => {
            const res = await axiosInstance.get<RecentActionsResponse>('/api/v1/dashboard/admin/recent-actions');
            return res.data;
        },
    });
}
