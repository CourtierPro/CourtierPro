import { useState, useEffect } from 'react';
import { Filter, Plus } from 'lucide-react';
import { getStagesForSide, enumToLabel, resolveStageIndex } from '@/shared/utils/stages';
import { useTranslation } from 'react-i18next';
import { PageHeader } from "@/shared/components/branded/PageHeader";
import { Section } from "@/shared/components/branded/Section";
import { LoadingState } from "@/shared/components/branded/LoadingState";
import { ErrorState } from "@/shared/components/branded/ErrorState";
import { Button } from "@/shared/components/ui/button";
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

  const handleSideFilterChange = (value: 'all' | 'buy' | 'sell') => {
    setSideFilter(value);
    if (value !== 'all') {
      setStageFilter('all');
    }
    setCurrentPage(1);
  };

  const handleStatusFilterChange = (value: 'all' | 'active' | 'closed' | 'terminated') => {
    setStatusFilter(value);
    setCurrentPage(1);
  };

  const handleStageFilterChange = (value: string) => {
    setStageFilter(value);
    setCurrentPage(1);
  };

  const handleSortByChange = (value: 'dateAsc' | 'dateDesc' | 'nameAsc' | 'nameDesc') => {
    setSortBy(value);
    setCurrentPage(1);
  };

  const getStageName = (tx: Transaction) => {
    const stageEnums = getStagesForSide(tx.side);
    const idx = resolveStageIndex(tx.currentStage, stageEnums);
    return enumToLabel(stageEnums[idx]);
  };

  const filteredTransactions = transactions.filter((transaction) => {
    if (sideFilter !== 'all') {
      if (sideFilter === 'buy' && transaction.side !== 'BUY_SIDE') return false;
      if (sideFilter === 'sell' && transaction.side !== 'SELL_SIDE') return false;
    }

    if (statusFilter !== 'all' && transaction.status !== statusFilter) return false;

    if (stageFilter !== 'all' && getStageName(transaction) !== stageFilter) return false;

    return true;
  });

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

  if (error && !isLoading) {
    return (
      <div className="space-y-6">
        <PageHeader
          title={t('transactionsTitle')}
          actions={
            <Button
              onClick={() => onNavigate('/transactions/new')}
              className="gap-2"
            >
              <Plus className="w-4 h-4" />
              {t('newTransaction')}
            </Button>
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
      <PageHeader
        title={t('title')}
        subtitle={t('subtitle')}
        actions={
          <Button
            onClick={() => onNavigate('/transactions/new')}
            className="gap-2"
          >
            <Plus className="w-4 h-4" />
            {t('newTransaction')}
          </Button>
        }
      />

      <TransactionFilters
        sideFilter={sideFilter}
        statusFilter={statusFilter}
        stageFilter={stageFilter}
        sortBy={sortBy}
        onSideFilterChange={handleSideFilterChange}
        onStatusFilterChange={handleStatusFilterChange}
        onStageFilterChange={handleStageFilterChange}
        onSortByChange={handleSortByChange}
        onResetFilters={handleResetFilters}
        hasActiveFilters={hasActiveFilters}
      />

      {paginatedTransactions.length === 0 ? (
        <Section className="p-12 text-center">
          <Filter className="w-16 h-16 mx-auto mb-4 text-muted-foreground/30" />
          <h2 className="mb-4 text-foreground font-medium">
            {t('noTransactions')}
          </h2>
          <Button onClick={handleResetFilters}>
            {t('resetFilters')}
          </Button>
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

    </div>
  );
}