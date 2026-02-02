import { useState, useEffect, useMemo } from 'react';
import { Filter, Plus, Archive, Search } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { PageHeader } from "@/shared/components/branded/PageHeader";
import { Section } from "@/shared/components/branded/Section";
import { LoadingState } from "@/shared/components/branded/LoadingState";
import { ErrorState } from "@/shared/components/branded/ErrorState";
import { Button } from "@/shared/components/ui/button";
import { Input } from "@/shared/components/ui/input";
import { useTransactions, usePinnedTransactionIds, useArchivedTransactions } from '@/features/transactions/api/queries';
import { TransactionFilters } from './TransactionFilters';
import { TransactionTable } from './TransactionTable';
import { TransactionCards } from './TransactionCards';
import { TransactionPagination } from './TransactionPagination';

interface TransactionListProps {
  language: 'en' | 'fr';
  onNavigate: (route: string) => void;
}

const ITEMS_PER_PAGE = 10;

import { parseToTimestamp } from '@/shared/utils/date';
import { CreateTransactionModal } from './CreateTransactionModal';


export function TransactionList({ language, onNavigate }: TransactionListProps) {
  const [sideFilter, setSideFilter] = useState<'all' | 'buy' | 'sell'>('all');
  const [statusFilter, setStatusFilter] = useState<'all' | 'ACTIVE' | 'CLOSED_SUCCESSFULLY' | 'TERMINATED_EARLY'>('ACTIVE');
  const [stageFilter, setStageFilter] = useState<string>('all');
  const [sortBy, setSortBy] = useState<'dateAsc' | 'dateDesc'>('dateDesc');
  const [currentPage, setCurrentPage] = useState(1);
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [showArchived, setShowArchived] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');

  const { data: transactions = [], isLoading, error, refetch } = useTransactions({
    status: statusFilter,
    stage: stageFilter,
    side: sideFilter
  });
  const { data: archivedTransactions = [], isLoading: isLoadingArchived } = useArchivedTransactions();
  const { data: pinnedIds = new Set<string>() } = usePinnedTransactionIds();

  const { t, i18n } = useTranslation('transactions');

  // Use archived transactions when viewing archived, otherwise use regular transactions
  const displayTransactions = showArchived ? archivedTransactions : transactions;
  const isLoadingData = showArchived ? isLoadingArchived : isLoading;

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

  const handleStatusFilterChange = (value: 'all' | 'ACTIVE' | 'CLOSED_SUCCESSFULLY' | 'TERMINATED_EARLY') => {
    setStatusFilter(value);
    setCurrentPage(1);
  };

  const handleStageFilterChange = (value: string) => {
    setStageFilter(value);
    setCurrentPage(1);
  };

  const handleSortByChange = (value: 'dateAsc' | 'dateDesc') => {
    setSortBy(value);
    setCurrentPage(1);
  };

  const handleToggleArchived = () => {
    setShowArchived(!showArchived);
    setCurrentPage(1);
  };


  const sortedTransactions = [...displayTransactions].sort((a, b) => {
    // Pinned items always come first
    const aPinned = pinnedIds.has(a.transactionId);
    const bPinned = pinnedIds.has(b.transactionId);
    if (aPinned && !bPinned) return -1;
    if (!aPinned && bPinned) return 1;

    // fallback to openedDate if openedAt is missing
    const aRaw = a.openedAt ?? a.openedDate ?? '';
    const bRaw = b.openedAt ?? b.openedDate ?? '';

    const aTime = parseToTimestamp(aRaw);
    const bTime = parseToTimestamp(bRaw);

    switch (sortBy) {
      case 'dateAsc':
        return aTime - bTime;

      case 'dateDesc':
      default:
        return bTime - aTime;
    }
  });

  // Filter by search term
  const filteredTransactions = useMemo(() => {
    if (!searchTerm.trim()) return sortedTransactions;
    const query = searchTerm.toLowerCase();
    return sortedTransactions.filter(tx => {
      const clientName = tx.clientName?.toLowerCase() || '';
      const streetAddress = tx.propertyAddress?.street?.toLowerCase() || '';
      const city = tx.propertyAddress?.city?.toLowerCase() || '';
      return clientName.includes(query) || streetAddress.includes(query) || city.includes(query);
    });
  }, [sortedTransactions, searchTerm]);


  const totalPages = Math.ceil(filteredTransactions.length / ITEMS_PER_PAGE);
  const startIndex = (currentPage - 1) * ITEMS_PER_PAGE;
  const endIndex = startIndex + ITEMS_PER_PAGE;
  const paginatedTransactions = filteredTransactions.slice(startIndex, endIndex);

  const handleResetFilters = () => {
    setSideFilter('all');
    setStatusFilter('all');
    setStageFilter('all');
    setSortBy('dateDesc');
    setSearchTerm('');
    setCurrentPage(1);
  };

  const handleSearchChange = (value: string) => {
    setSearchTerm(value);
    setCurrentPage(1);
  };

  const hasActiveFilters = sideFilter !== 'all' || statusFilter !== 'all' || stageFilter !== 'all' || searchTerm.trim() !== '';

  if (error && !isLoadingData) {
    return (
      <div className="space-y-6">
        <PageHeader
          title={t('transactionsTitle')}
          actions={
            <Button
              onClick={() => setIsCreateModalOpen(true)}
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

  if (isLoadingData) {
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
        title={showArchived ? t('archivedTransactions') : t('title')}
        subtitle={showArchived ? t('archivedSubtitle') : t('subtitle')}
        actions={
          <div className="flex gap-2">
            <Button
              variant={showArchived ? "default" : "outline"}
              onClick={handleToggleArchived}
              className="gap-2"
            >
              <Archive className="w-4 h-4" />
              {showArchived ? t('viewActive') : t('viewArchived')}
            </Button>
            {!showArchived && (
              <Button
                onClick={() => setIsCreateModalOpen(true)}
                className="gap-2"
              >
                <Plus className="w-4 h-4" />
                {t('newTransaction')}
              </Button>
            )}
          </div>
        }
      />

      {!showArchived && (
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
      )}

      {/* Search Input */}
      <Section className="p-4">
        <div className="relative">
          <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-muted-foreground" />
          <Input
            type="text"
            placeholder={t('searchPlaceholder', { defaultValue: 'Search by client name or address...' })}
            value={searchTerm}
            onChange={(e) => handleSearchChange(e.target.value)}
            className="pl-9"
          />
        </div>
      </Section>

      {paginatedTransactions.length === 0 ? (
        <Section className="p-12 text-center">
          <Filter className="w-16 h-16 mx-auto mb-4 text-muted-foreground/30" />
          <h2 className="mb-4 text-foreground font-medium">
            {showArchived ? t('noArchivedTransactions') : t('noTransactions')}
          </h2>
          {!showArchived && (
            <Button onClick={handleResetFilters}>
              {t('resetFilters')}
            </Button>
          )}
        </Section>
      ) : (
        <>
          <TransactionTable transactions={paginatedTransactions} onNavigate={onNavigate} pinnedIds={pinnedIds} showArchived={showArchived} />
          <TransactionCards transactions={paginatedTransactions} onNavigate={onNavigate} pinnedIds={pinnedIds} showArchived={showArchived} />
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

      <CreateTransactionModal
        isOpen={isCreateModalOpen}
        onClose={() => setIsCreateModalOpen(false)}
        onSuccess={() => {
          setIsCreateModalOpen(false);
          // Optionally refetch transactions if we stayed on list, but form navigates away usually.
          // If the form behavior changes to stay on list, we would refetch().
        }}
      />
    </div>
  );
}