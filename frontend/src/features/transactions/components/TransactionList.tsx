import { useState, useEffect } from 'react';
import { Filter, Plus } from 'lucide-react';
import { getStagesForSide, enumToLabel, resolveStageIndex } from '@/shared/utils/stages';
import { useTranslation } from 'react-i18next';
import { PageHeader } from "@/shared/components/branded/PageHeader";
import { Section } from "@/shared/components/branded/Section";
import { LoadingState } from "@/shared/components/branded/LoadingState";
import { ErrorState } from "@/shared/components/branded/ErrorState";
import { useTransactions, type Transaction } from '@/features/transactions/api/queries';
import { TransactionFilters } from './TransactionFilters';
import { TransactionTable } from './TransactionTable';
import { TransactionCards } from './TransactionCards';
import { TransactionPagination } from './TransactionPagination';

interface TransactionListProps {
  language: 'en' | 'fr';
  onNavigate: (route: string) => void;
}

const ITEMS_PER_PAGE = 10;

export function TransactionList({ language, onNavigate }: TransactionListProps) {
  const { data: transactions = [], isLoading, error, refetch } = useTransactions();

  const [sideFilter, setSideFilter] = useState<'all' | 'buy' | 'sell'>('all');
  const [statusFilter, setStatusFilter] = useState<'all' | 'active' | 'closed' | 'terminated'>('all');
  const [stageFilter, setStageFilter] = useState<string>('all');
  const [sortBy, setSortBy] = useState<'dateAsc' | 'dateDesc' | 'nameAsc' | 'nameDesc'>('dateDesc');
  const [currentPage, setCurrentPage] = useState(1);

  const { t, i18n } = useTranslation('transactions');

  useEffect(() => {
    if (language) {
      i18n.changeLanguage(language);
    }
  }, [language, i18n]);

  // Reset page when filters change
  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setCurrentPage(1);
  }, [sideFilter, statusFilter, stageFilter, sortBy]);

  // Reset stage filter when side filter changes
  useEffect(() => {
    if (sideFilter !== 'all') {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setStageFilter('all');
    }
  }, [sideFilter]);

  const getStageName = (tx: Transaction) => {
    const stageEnums = getStagesForSide(tx.side);
    const idx = resolveStageIndex(tx.currentStage, stageEnums);
    return enumToLabel(stageEnums[idx]);
  };

  // Filter transactions
  const filteredTransactions = transactions.filter((transaction) => {
    if (sideFilter !== 'all') {
      if (sideFilter === 'buy' && transaction.side !== 'BUY_SIDE') return false;
      if (sideFilter === 'sell' && transaction.side !== 'SELL_SIDE') return false;
    }

    if (statusFilter !== 'all' && transaction.status !== statusFilter) return false;

    if (stageFilter !== 'all' && getStageName(transaction) !== stageFilter) return false;

    return true;
  });

  // Sort transactions
  const sortedTransactions = [...filteredTransactions].sort((a, b) => {
    switch (sortBy) {
      case 'dateAsc':
        return new Date(a.openedAt).getTime() - new Date(b.openedAt).getTime();
      case 'dateDesc':
        return new Date(b.openedAt).getTime() - new Date(a.openedAt).getTime();
      // Disable name sorting until clientName exists
      case 'nameAsc':
      case 'nameDesc':
        return 0;
      default:
        return 0;
    }
  });

  // Pagination
  const totalPages = Math.ceil(sortedTransactions.length / ITEMS_PER_PAGE);
  const startIndex = (currentPage - 1) * ITEMS_PER_PAGE;
  const endIndex = startIndex + ITEMS_PER_PAGE;
  const paginatedTransactions = sortedTransactions.slice(startIndex, endIndex);

  const handleResetFilters = () => {
    setSideFilter('all');
    setStatusFilter('all');
    setStageFilter('all');
    setSortBy('dateDesc');
    setCurrentPage(1);
  };

  const hasActiveFilters = sideFilter !== 'all' || statusFilter !== 'all' || stageFilter !== 'all';

  // Error state
  if (error && !isLoading) {
    return (
      <div className="space-y-6">
        <PageHeader
          title={t('transactionsTitle')}
          actions={
            <button
              onClick={() => onNavigate('/transactions/new')}
              className="flex items-center gap-2 px-4 py-2 bg-primary text-primary-foreground rounded-lg hover:opacity-90 transition-opacity"
            >
              <Plus className="w-4 h-4" />
              {t('newTransaction')}
            </button>
          }
        />
        <ErrorState message={error.message || "Failed to load transactions"} onRetry={() => refetch()} />
      </div>
    );
  }

  if (isLoading) {
    return (
      <div className="space-y-6">
        <PageHeader title={t('title')} subtitle={t('subtitle')} />
        <LoadingState message={t('loading')} />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <PageHeader title={t('title')} subtitle={t('subtitle')} />

      {/* Filters and Sorting */}
      <TransactionFilters
        sideFilter={sideFilter}
        statusFilter={statusFilter}
        stageFilter={stageFilter}
        sortBy={sortBy}
        onSideFilterChange={setSideFilter}
        onStatusFilterChange={setStatusFilter}
        onStageFilterChange={setStageFilter}
        onSortByChange={setSortBy}
        onResetFilters={handleResetFilters}
        hasActiveFilters={hasActiveFilters}
      />

      {/* Transaction List */}
      {paginatedTransactions.length === 0 ? (
        <Section className="p-12 text-center">
          <Filter className="w-16 h-16 mx-auto mb-4 text-muted-foreground/30" />
          <h2 className="mb-4 text-foreground font-medium">
            {t('noTransactions')}
          </h2>
          <button
            onClick={handleResetFilters}
            className="py-2 px-6 rounded-lg hover:opacity-90 focus:outline-none focus:ring-2 focus:ring-primary focus:ring-offset-2 transition-all bg-primary text-primary-foreground"
          >
            {t('resetFilters')}
          </button>
        </Section>
      ) : (
        <>
          <TransactionTable transactions={paginatedTransactions} onNavigate={onNavigate} />
          <TransactionCards transactions={paginatedTransactions} onNavigate={onNavigate} />
          <TransactionPagination
            currentPage={currentPage}
            totalPages={totalPages}
            startIndex={startIndex}
            endIndex={endIndex}
            totalItems={sortedTransactions.length}
            onPageChange={setCurrentPage}
          />
        </>
      )}

      {/* Floating Action Button (Create New) */}
      <button
        onClick={() => onNavigate('/transactions/new')}
        aria-label={t('createNew')}
        className="fixed bottom-6 right-6 z-50 flex items-center gap-2 px-4 py-3 rounded-full shadow-lg hover:shadow-2xl focus:outline-none focus:ring-2 focus:ring-primary transition-transform bg-primary text-primary-foreground"
      >
        <Plus className="w-5 h-5" />
        <span className="font-bold hidden sm:inline">{t('createNew')}</span>
      </button>
    </div>
  );
}