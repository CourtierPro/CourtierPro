import { useEffect, useMemo, useState } from 'react';
import { Filter } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { PageHeader } from "@/shared/components/branded/PageHeader";
import { Section } from "@/shared/components/branded/Section";
import { LoadingState } from "@/shared/components/branded/LoadingState";
import { ErrorState } from "@/shared/components/branded/ErrorState";
import { Button } from "@/shared/components/ui/button";
import { TransactionFilters } from './TransactionFilters';
import { ClientTransactionTable } from './ClientTransactionTable';
import { ClientTransactionCards } from './ClientTransactionCards';
import { TransactionPagination } from './TransactionPagination';
import { useClientTransactionsPageLogic } from '../hooks/useClientTransactionsPageLogic';
import { parseToTimestamp } from '@/shared/utils/date';
import { useLanguage } from '@/app/providers/LanguageContext';
import { getStagesForSide, enumToLabel, resolveStageIndex } from '@/shared/utils/stages';

const ITEMS_PER_PAGE = 10;

interface ClientTransactionListProps {
  onNavigate: (route: string) => void;
}

export function ClientTransactionList({ onNavigate }: ClientTransactionListProps) {
  const [sideFilter, setSideFilter] = useState<'all' | 'buy' | 'sell'>('all');
  const [statusFilter, setStatusFilter] = useState<'all' | 'active' | 'closed' | 'terminated'>('all');
  const [stageFilter, setStageFilter] = useState<string>('all');
  const [sortBy, setSortBy] = useState<'dateAsc' | 'dateDesc'>('dateDesc');
  const [currentPage, setCurrentPage] = useState(1);

  const { t, i18n } = useTranslation('transactions');
  const { language } = useLanguage();
  const { transactions = [], isLoading, error, refetch } = useClientTransactionsPageLogic();

  useEffect(() => {
    if (language) {
      i18n.changeLanguage(language);
    }
  }, [language, i18n]);

  const filteredTransactions = useMemo(() => {
    return transactions
      .filter((tx) => (sideFilter === 'all' ? true : sideFilter === 'buy' ? tx.side === 'BUY_SIDE' : tx.side === 'SELL_SIDE'))
      .filter((tx) => (statusFilter === 'all' ? true : tx.status === statusFilter))
      .filter((tx) => {
        if (stageFilter === 'all') return true;
        const stageEnums = getStagesForSide(tx.side);
        const idx = resolveStageIndex(tx.currentStage, stageEnums);
        if (idx < 0) return false;
        const label = enumToLabel(stageEnums[idx]);
        return label === stageFilter;
      });
  }, [transactions, sideFilter, statusFilter, stageFilter]);

  const sortedTransactions = useMemo(() => {
    return [...filteredTransactions].sort((a, b) => {
      const aRaw = a.openedAt ?? a.openedDate ?? '';
      const bRaw = b.openedAt ?? b.openedDate ?? '';

      const aTime = parseToTimestamp(aRaw);
      const bTime = parseToTimestamp(bRaw);

      switch (sortBy) {
        case 'dateAsc':
          return aTime - bTime;
        case 'dateDesc':
          return bTime - aTime;
        default:
          return 0;
      }
    });
  }, [filteredTransactions, sortBy]);

  const totalPages = Math.max(1, Math.ceil(sortedTransactions.length / ITEMS_PER_PAGE));
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
        <PageHeader title={t('title')} subtitle={t('subtitle')} />
        <ErrorState message={error.message || t('errorLoadingTransactions')} onRetry={() => refetch()} />
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
      <PageHeader title={t('title')} subtitle={t('subtitle')} />

      <TransactionFilters
        sideFilter={sideFilter}
        statusFilter={statusFilter}
        stageFilter={stageFilter}
        sortBy={sortBy}
        onSideFilterChange={(value) => {
          setSideFilter(value);
          if (value !== 'all') setStageFilter('all');
          setCurrentPage(1);
        }}
        onStatusFilterChange={(value) => {
          setStatusFilter(value);
          setCurrentPage(1);
        }}
        onStageFilterChange={(value) => {
          setStageFilter(value);
          setCurrentPage(1);
        }}
        onSortByChange={(value) => {
          setSortBy(value);
          setCurrentPage(1);
        }}
        onResetFilters={handleResetFilters}
        hasActiveFilters={hasActiveFilters}
      />

      {paginatedTransactions.length === 0 ? (
        <Section className="p-12 text-center">
          <Filter className="w-16 h-16 mx-auto mb-4 text-muted-foreground/30" />
          <h2 className="mb-4 text-foreground font-medium">{t('noTransactions')}</h2>
          <Button onClick={handleResetFilters}>{t('resetFilters')}</Button>
        </Section>
      ) : (
        <>
          <ClientTransactionTable transactions={paginatedTransactions} onNavigate={onNavigate} />
          <ClientTransactionCards transactions={paginatedTransactions} onNavigate={onNavigate} />
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
