
import React, { useState, useMemo } from 'react';
import { useQueries } from '@tanstack/react-query';
import { useCurrentUser } from '@/features/auth/api/useCurrentUser';
import { useTransactions, useClientTransactions } from '@/features/transactions/api/queries';
import { documentKeys } from '@/features/documents/api/queries';
import { TransactionDocumentSelector } from '@/features/documents/components/TransactionDocumentSelector';
import { DocumentsPage } from './DocumentsPage';
import { LoadingState } from '@/shared/components/branded/LoadingState';
import { ErrorState } from '@/shared/components/branded/ErrorState';
import axiosInstance from '@/shared/api/axiosInstance';
import type { DocumentRequest } from '@/features/documents/types';






export const AllDocumentsPage: React.FC = () => {
  const { data: currentUser, isLoading: isLoadingUser, error: errorUser } = useCurrentUser();
  const [selectedTransactionId, setSelectedTransactionId] = useState<string | null>(null);
  const [selectedTransactionSide, setSelectedTransactionSide] = useState<'BUY_SIDE' | 'SELL_SIDE' | null>(null);

  const role = currentUser?.role?.toLowerCase();

  const { data: brokerTransactions = [], isLoading: isBrokerLoading, error: brokerError } = useTransactions();
  const { data: clientTransactions = [], isLoading: isClientLoading, error: clientError } = useClientTransactions(currentUser?.id ?? '');

  const transactions = role === 'broker' ? brokerTransactions : clientTransactions;
  const isTxLoading = role === 'broker' ? isBrokerLoading : isClientLoading;
  const txError = role === 'broker' ? brokerError : clientError;

  // Load stats for all transactions using useQueries
  const documentStatsQueries = useQueries({
    queries: transactions.map(tx => ({
      queryKey: documentKeys.stat(tx.transactionId),
      queryFn: async () => {
        const response = await axiosInstance.get<DocumentRequest[]>(`/transactions/${tx.transactionId}/documents`);
        const docs = response.data || [];
        
        const pending = docs.filter((d) => d.status === 'REQUESTED').length;
        const submitted = docs.filter((d) => d.status === 'SUBMITTED').length;
        const approved = docs.filter((d) => d.status === 'APPROVED').length;
        const needsRevision = docs.filter((d) => d.status === 'NEEDS_REVISION').length;
        
        return {
          count: docs.length,
          statuses: { pending, submitted, approved, needsRevision }
        };
      },
      enabled: !!tx.transactionId,
    })),
  });

  const transactionOptions = useMemo(() => {
    if (role === 'broker') {
      const grouped: Record<string, { clientName: string; transactions: Array<typeof transactions[number] & { documentCount?: number; documentStatuses?: { pending: number; submitted: number; approved: number; needsRevision: number } }> }> = {};
      transactions.forEach((tx, index) => {
        const statsData = documentStatsQueries[index]?.data;
        if (!grouped[tx.clientId]) {
          grouped[tx.clientId] = { clientName: tx.clientName, transactions: [] };
        }
        grouped[tx.clientId].transactions.push({
          ...tx,
          documentCount: statsData?.count,
          documentStatuses: statsData?.statuses,
        });
      });
      return Object.values(grouped).flatMap(group =>
        group.transactions.map(tx => ({
          transactionId: tx.transactionId,
          clientName: group.clientName,
          side: tx.side,
          propertyAddress: tx.propertyAddress?.street,
          currentStage: tx.currentStage ? String(tx.currentStage) : undefined,
          documentCount: tx.documentCount,
          documentStatuses: tx.documentStatuses,
        }))
      );
    } else {
      // Client : simple liste
      return transactions.map((tx, index) => {
        const statsData = documentStatsQueries[index]?.data;
        return {
          transactionId: tx.transactionId,
          clientName: tx.clientName,
          side: tx.side,
          propertyAddress: tx.propertyAddress?.street,
          currentStage: tx.currentStage ? String(tx.currentStage) : undefined,
          documentCount: statsData?.count,
          documentStatuses: statsData?.statuses,
        };
      });
    }
  }, [transactions, role, documentStatsQueries]);

  if (isLoadingUser || isTxLoading) return <LoadingState />;
  if (errorUser) return <ErrorState message={errorUser.message} />;
  if (txError) return <ErrorState message={txError.message} />;

  const handleSelect = (id: string, side: 'BUY_SIDE' | 'SELL_SIDE') => {
    setSelectedTransactionId(id);
    setSelectedTransactionSide(side);
  };

  return (
    <div className="max-w-5xl mx-auto px-4 py-8">
      <TransactionDocumentSelector
        transactions={transactionOptions}
        onSelect={handleSelect}
        selectedTransactionId={selectedTransactionId || undefined}
      />
      {selectedTransactionId && selectedTransactionSide && (
        <DocumentsPage
          transactionId={selectedTransactionId}
          transactionSide={selectedTransactionSide}
          hideRequestButton={true}
        />
      )}
    </div>
  );
};
