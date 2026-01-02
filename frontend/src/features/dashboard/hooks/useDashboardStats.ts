import { useQuery } from '@tanstack/react-query';
import { useClientTransactions } from '@/features/transactions/api/queries';
import { fetchAllDocuments } from '@/features/documents/api/documentsApi';
import type { DocumentRequest } from '@/features/documents/types';
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
}

export interface AdminDashboardStats {
    totalUsers: number;
    activeBrokers: number;
    systemHealth: string;
}

import { useState, useMemo } from 'react';

export function useClientDashboardStats(clientId: string) {
    // Transactions du client
    const { data: transactions = [], isLoading: isLoadingTransactions, error: errorTransactions } = useClientTransactions(clientId);
    // Tous les documents (filtrage côté client)
    const { data: allDocuments = [], isLoading: isLoadingDocuments, error: errorDocuments } = useQuery<DocumentRequest[]>({
        queryKey: ['all-documents'],
        queryFn: fetchAllDocuments,
    });
    // Filtrer les documents pour ne garder que ceux du client connecté
    const documents = allDocuments.filter(d => d.transactionRef?.clientId === clientId);

    // Sélection de la transaction (par défaut la première active, sinon la première)
    const [selectedTransactionId, setSelectedTransactionId] = useState<string | null>(null);
    const selectedTransaction = useMemo(() => {
        if (!transactions.length) return null;
        if (selectedTransactionId) return transactions.find(t => t.transactionId === selectedTransactionId) || transactions[0];
        // Par défaut, la première active, sinon la première
        return transactions.find(t => t.status === 'ACTIVE') || transactions[0];
    }, [transactions, selectedTransactionId]);

    // Documents de la transaction sélectionnée
    const selectedDocuments = useMemo(() => {
        if (!selectedTransaction) return [];
        return documents.filter(d => d.transactionRef?.transactionId === selectedTransaction.transactionId);
    }, [documents, selectedTransaction]);

    // KPIs globaux (toutes transactions)
    const activeTransactions = transactions.filter(t => t.status === 'ACTIVE');
    const documentsNeededGlobal = documents.filter(d => d.status === 'REQUESTED');
    const documentsSubmittedGlobal = documents.filter(d => d.status === 'SUBMITTED');

    // KPIs de la transaction sélectionnée
    const documentsNeeded = selectedDocuments.filter(d => d.status === 'REQUESTED');
    const documentsSubmitted = selectedDocuments.filter(d => d.status === 'SUBMITTED');

    return {
        transactions,
        selectedTransaction,
        setSelectedTransactionId,
        selectedTransactionId,
        kpis: {
            global: {
                activeTransactions: activeTransactions.length,
                documentsNeeded: documentsNeededGlobal.length,
                documentsSubmitted: documentsSubmittedGlobal.length,
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
