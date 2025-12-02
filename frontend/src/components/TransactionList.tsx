import { useState, useEffect } from 'react';
import { ChevronLeft, ChevronRight, Filter, X, Home, Users, Plus } from 'lucide-react';
import axiosInstance from "@/api/axiosInstance";
import { getStagesForSide, enumToLabel, resolveStageIndex } from '@/utils/stages';
import { useTranslation } from 'react-i18next';

interface TransactionListProps {
  language: 'en' | 'fr';
  onNavigate: (route: string) => void;
}

interface Transaction {
  transactionId: string;
  clientId: string;
  propertyAddress: {
    street: string;
    city: string;
    province: string;
    postalCode: string;
  };
  side: 'BUY_SIDE' | 'SELL_SIDE';
  currentStage: number;
  totalStages: number;
  status: 'active' | 'closed' | 'terminated';
  openedAt: string;
  openedDate?: string;
}
// translations are loaded from i18n namespace `transactions`



const ITEMS_PER_PAGE = 10;

export function TransactionList({ language, onNavigate }: TransactionListProps) {
  const [isLoading, setIsLoading] = useState(true);
  const [transactions, setTransactions] = useState<Transaction[]>([]);
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
    setCurrentPage(1);
  }, [sideFilter, statusFilter, stageFilter, sortBy]);

  // Reset stage filter when side filter changes
  useEffect(() => {
    setStageFilter('all');
  }, [sideFilter]);

  // Load transactions from backend
  useEffect(() => {
    const loadTransactions = async () => {
      try {
        const res = await axiosInstance.get('/transactions', {
          headers: { 'x-broker-id': 'BROKER1' },
        });

        setTransactions(res.data);
      } catch (err) {
        console.error('Failed to load transactions:', err);
      } finally {
        setIsLoading(false);
      }
    };

    loadTransactions();
  }, []);

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

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'active':
        return '#10b981';
      case 'closed':
        return '#6b7280';
      case 'terminated':
        return '#ef4444';
      default:
        return '#353535';
    }
  };

  const hasActiveFilters = sideFilter !== 'all' || statusFilter !== 'all' || stageFilter !== 'all';

  // Get available stages based on selected side (deduplicated labels)
  const availableStages = (() => {
    if (sideFilter === 'buy') return getStagesForSide('BUY_SIDE').map(enumToLabel);
    if (sideFilter === 'sell') return getStagesForSide('SELL_SIDE').map(enumToLabel);
    const combined = [...getStagesForSide('BUY_SIDE'), ...getStagesForSide('SELL_SIDE')];
    // Deduplicate by enum
    const uniqueEnums = Array.from(new Set(combined));
    return uniqueEnums.map(enumToLabel);
  })();

  if (isLoading) {
    return (
      <div className="space-y-6">
        <div>
          <h1 style={{ color: '#353535' }}>{t('title')}</h1>
          <p style={{ color: '#353535', opacity: 0.7 }}>{t('subtitle')}</p>
        </div>
        <div className="flex items-center justify-center h-64">
          <div className="text-center">
            <div
              className="w-12 h-12 border-4 border-t-transparent rounded-full animate-spin mx-auto mb-4"
              style={{ borderColor: '#FF6B01', borderTopColor: 'transparent' }}
            />
            <p style={{ color: '#353535' }}>{t('loading')}</p>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="sticky top-0 z-20 bg-white py-4">
        <div className="flex items-start justify-between gap-4">
          <div>
            <h1 style={{ color: '#353535' }}>{t('title')}</h1>
            <p style={{ color: '#353535', opacity: 0.7 }}>{t('subtitle')}</p>
          </div>
        </div>
      </div>

      {/* Filters and Sorting */}
      <div
        className="p-4 rounded-xl shadow-md"
        style={{ backgroundColor: '#FFFFFF' }}
        role="region"
        aria-label="Filter and sort controls"
      >
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-4">
          {/* Transaction Side Filter */}
          <div>
            <label
              htmlFor="side-filter"
              style={{ color: '#353535', fontSize: '0.875rem' }}
              className="block mb-2"
            >
              {t('transactionSide')}
            </label>
            <select
              id="side-filter"
              value={sideFilter}
              onChange={(e) => setSideFilter(e.target.value as 'all' | 'buy' | 'sell')}
              className="w-full p-2 rounded-lg border-2 border-gray-200 focus:outline-none focus:ring-2 focus:ring-[#FF6B01] focus:border-transparent"
              style={{ color: '#353535' }}
            >
              <option value="all">{t('all')}</option>
              <option value="buy">{t('buy')}</option>
              <option value="sell">{t('sell')}</option>
            </select>
          </div>

          {/* Status Filter */}
          <div>
            <label
              htmlFor="status-filter"
              style={{ color: '#353535', fontSize: '0.875rem' }}
              className="block mb-2"
            >
              {t('status')}
            </label>
            <select
              id="status-filter"
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value as 'all' | 'active' | 'closed' | 'terminated')}
              className="w-full p-2 rounded-lg border-2 border-gray-200 focus:outline-none focus:ring-2 focus:ring-[#FF6B01] focus:border-transparent"
              style={{ color: '#353535' }}
            >
              <option value="all">{t('all')}</option>
              <option value="active">{t('active')}</option>
              <option value="closed">{t('closed')}</option>
              <option value="terminated">{t('terminated')}</option>
            </select>
          </div>

          {/* Stage Filter */}
          <div>
            <label
              htmlFor="stage-filter"
              style={{ color: '#353535', fontSize: '0.875rem' }}
              className="block mb-2"
            >
              {t('stage')}
            </label>
            <select
              id="stage-filter"
              value={stageFilter}
              onChange={(e) => setStageFilter(e.target.value)}
              className="w-full p-2 rounded-lg border-2 border-gray-200 focus:outline-none focus:ring-2 focus:ring-[#FF6B01] focus:border-transparent"
              style={{ color: '#353535' }}
              disabled={sideFilter === 'all'}
            >
              <option value="all">{t('all')}</option>
              {availableStages.map((stage) => (
                <option key={stage} value={stage}>
                  {stage}
                </option>
              ))}
            </select>
          </div>

          {/* Sort By */}
          <div>
            <label
              htmlFor="sort-by"
              style={{ color: '#353535', fontSize: '0.875rem' }}
              className="block mb-2"
            >
              {t('sortBy')}
            </label>
            <select
              id="sort-by"
              value={sortBy}
              onChange={(e) => setSortBy(e.target.value as any)}
              className="w-full p-2 rounded-lg border-2 border-gray-200 focus:outline-none focus:ring-2 focus:ring-[#FF6B01] focus:border-transparent"
              style={{ color: '#353535' }}
            >
              <option value="dateDesc">{t('dateOpenedDesc')}</option>
              <option value="dateAsc">{t('dateOpenedAsc')}</option>
              {/* Disabled until backend returns clientName */}
              <option value="nameAsc" disabled>
                {t('clientNameAsc')} (Not available)
              </option>
              <option value="nameDesc" disabled>
                {t('clientNameDesc')} (Not available)
              </option>
            </select>
          </div>

          {/* Clear Filters Button */}
          <div className="flex items-end">
            <button
              onClick={handleResetFilters}
              disabled={!hasActiveFilters}
              className="w-full p-2 rounded-lg hover:opacity-90 focus:outline-none focus:ring-2 focus:ring-[#FF6B01] focus:ring-offset-2 transition-all disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
              style={{
                backgroundColor: hasActiveFilters ? '#FF6B01' : '#e5e7eb',
                color: hasActiveFilters ? '#FFFFFF' : '#9ca3af',
              }}
            >
              <X className="w-4 h-4" />
              {t('clearFilters')}
            </button>
          </div>
        </div>
      </div>

      {/* Transaction List */}
      {paginatedTransactions.length === 0 ? (
        <div
          className="p-12 rounded-xl shadow-md text-center"
          style={{ backgroundColor: '#FFFFFF' }}
        >
          <Filter className="w-16 h-16 mx-auto mb-4" style={{ color: '#353535', opacity: 0.3 }} />
          <h2 style={{ color: '#353535' }} className="mb-4">
            {t('noTransactions')}
          </h2>
          <button
            onClick={handleResetFilters}
            className="py-2 px-6 rounded-lg hover:opacity-90 focus:outline-none focus:ring-2 focus:ring-[#FF6B01] focus:ring-offset-2 transition-all"
            style={{ backgroundColor: '#FF6B01', color: '#FFFFFF' }}
          >
            {t('resetFilters')}
          </button>
        </div>
      ) : (
        <>
          {/* Desktop Table View */}
          <div className="hidden lg:block rounded-xl shadow-md overflow-hidden" style={{ backgroundColor: '#FFFFFF' }}>
            <div className="overflow-x-auto">
              <table className="w-full" role="table">
                <thead style={{ backgroundColor: '#f9fafb' }}>
                  <tr role="row">
                    <th className="p-4 text-left" style={{ color: '#353535' }} scope="col">
                      {t('clientName')}
                    </th>
                    <th className="p-4 text-left" style={{ color: '#353535' }} scope="col">
                      {t('propertyAddress')}
                    </th>
                    <th className="p-4 text-left" style={{ color: '#353535' }} scope="col">
                      {t('side')}
                    </th>
                    <th className="p-4 text-left" style={{ color: '#353535' }} scope="col">
                      {t('currentStage')}
                    </th>
                    <th className="p-4 text-left" style={{ color: '#353535' }} scope="col">
                      {t('statusLabel')}
                    </th>
                    <th className="p-4 text-left" style={{ color: '#353535' }} scope="col">
                      {t('openedDate')}
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {paginatedTransactions.map((transaction) => (
                    <tr
                      key={transaction.transactionId}
                      role="row"
                      onClick={() => {
                        if (transaction?.transactionId) onNavigate(`/transactions/${transaction.transactionId}`);
                      }}
                      onKeyDown={(e) => {
                        if (e.key === 'Enter' || e.key === ' ') {
                          e.preventDefault();
                          if (transaction?.transactionId) onNavigate(`/transactions/${transaction.transactionId}`);
                        }
                      }}
                      tabIndex={0}
                      className="border-t border-gray-100 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-inset focus:ring-[#FF6B01] cursor-pointer transition-colors"
                      aria-label={`View transaction for ${transaction.clientId}`}
                    >
                      <td className="p-4" style={{ color: '#353535' }}>
                        <div className="flex items-center gap-2">
                          <Users className="w-4 h-4" style={{ color: '#FF6B01' }} />
                          {transaction.clientId}
                        </div>
                      </td>
                      <td className="p-4" style={{ color: '#353535' }}>
                        <div className="flex items-center gap-2">
                          <Home className="w-4 h-4" style={{ color: '#353535', opacity: 0.5 }} />
                          {transaction.propertyAddress?.street}
                        </div>
                      </td>
                      <td className="p-4">
                        <span
                          className="px-3 py-1 rounded-full"
                          style={{
                            backgroundColor: transaction.side === 'BUY_SIDE' ? '#dbeafe' : '#fef3c7',
                            color: transaction.side === 'BUY_SIDE' ? '#1e40af' : '#92400e',
                            fontSize: '0.875rem',
                          }}
                        >
                          {transaction.side === 'BUY_SIDE' ? t('buy') : t('sell')}
                        </span>
                      </td>
                      <td className="p-4" style={{ color: '#353535' }}>
                        {getStageName(transaction)}
                      </td>
                      <td className="p-4">
                        <span
                          className="px-3 py-1 rounded-full"
                          style={{
                            backgroundColor: `${getStatusColor(transaction.status)}20`,
                            color: getStatusColor(transaction.status),
                            fontSize: '0.875rem',
                          }}
                        >
                          {t(String(transaction.status))}
                        </span>
                      </td>
                      <td className="p-4" style={{ color: '#353535', opacity: 0.7 }}>
                        {transaction.openedDate ?? (transaction.openedAt ? transaction.openedAt.substring(0, 10) : '')}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>

          {/* Mobile Card View */}
          <div className="lg:hidden space-y-4">
            {paginatedTransactions.map((transaction) => (
              <button
                key={transaction.transactionId}
                onClick={() => {
                  if (transaction?.transactionId) onNavigate(`/transactions/${transaction.transactionId}`);
                }}
                onKeyDown={(e) => {
                  if (e.key === 'Enter' || e.key === ' ') {
                    e.preventDefault();
                    if (transaction?.transactionId) onNavigate(`/transactions/${transaction.transactionId}`);
                  }
                }}
                className="w-full p-4 rounded-xl shadow-md hover:shadow-lg focus:outline-none focus:ring-2 focus:ring-[#FF6B01] focus:ring-offset-2 transition-all text-left"
                style={{ backgroundColor: '#FFFFFF' }}
                aria-label={`View transaction for ${transaction.clientId}`}
              >
                <div className="space-y-3">
                  <div className="flex items-start justify-between gap-2">
                    <div className="flex items-center gap-2 flex-1">
                      <Users className="w-5 h-5 flex-shrink-0" style={{ color: '#FF6B01' }} />
                      <p style={{ color: '#353535' }}>{transaction.clientId}</p>
                    </div>
                    <span
                      className="px-3 py-1 rounded-full flex-shrink-0"
                      style={{
                        backgroundColor: `${getStatusColor(transaction.status)}20`,
                        color: getStatusColor(transaction.status),
                        fontSize: '0.875rem',
                      }}
                    >
                      {t(String(transaction.status))}
                    </span>
                  </div>

                  <div className="flex items-start gap-2">
                    <Home className="w-4 h-4 flex-shrink-0 mt-1" style={{ color: '#353535', opacity: 0.5 }} />
                    <p style={{ color: '#353535', fontSize: '0.875rem', opacity: 0.7 }}>
                      {transaction.propertyAddress?.street}
                    </p>
                  </div>

                  <div className="flex items-center justify-between">
                    <span
                      className="px-3 py-1 rounded-full"
                      style={{
                        backgroundColor: transaction.side === 'BUY_SIDE' ? '#dbeafe' : '#fef3c7',
                        color: transaction.side === 'BUY_SIDE' ? '#1e40af' : '#92400e',
                        fontSize: '0.875rem',
                      }}
                    >
                      {transaction.side === 'BUY_SIDE' ? t('buy') : t('sell')}
                    </span>
                    <p style={{ color: '#353535', fontSize: '0.875rem' }}>
                      {getStageName(transaction)}
                    </p>
                  </div>

                  <p style={{ color: '#353535', fontSize: '0.875rem', opacity: 0.7 }}>
                    {t('openedDate')}: {transaction.openedAt}
                  </p>
                </div>
              </button>
            ))}
          </div>

          {/* Pagination */}
          {totalPages > 1 && (
            <div
              className="p-4 rounded-xl shadow-md flex items-center justify-between flex-wrap gap-4"
              style={{ backgroundColor: '#FFFFFF' }}
              role="navigation"
              aria-label="Pagination"
            >
              <p style={{ color: '#353535', fontSize: '0.875rem' }}>
                {t('showing')} {startIndex + 1} {t('to')} {Math.min(endIndex, sortedTransactions.length)} {t('of')}{' '}
                {sortedTransactions.length} {t('transactions')}
              </p>

              <div className="flex items-center gap-2">
                <button
                  onClick={() => setCurrentPage((prev) => Math.max(1, prev - 1))}
                  disabled={currentPage === 1}
                  className="p-2 rounded-lg hover:bg-gray-100 focus:outline-none focus:ring-2 focus:ring-[#FF6B01] disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                  style={{ color: '#353535' }}
                  aria-label={t('previous')}
                >
                  <ChevronLeft className="w-5 h-5" />
                </button>

                {Array.from({ length: totalPages }, (_, i) => i + 1).map((page) => (
                  <button
                    key={page}
                    onClick={() => setCurrentPage(page)}
                    className="w-10 h-10 rounded-lg hover:bg-gray-100 focus:outline-none focus:ring-2 focus:ring-[#FF6B01] transition-colors"
                    style={{
                      backgroundColor: currentPage === page ? '#FF6B01' : 'transparent',
                      color: currentPage === page ? '#FFFFFF' : '#353535',
                    }}
                    aria-label={`Page ${page}`}
                    aria-current={currentPage === page ? 'page' : undefined}
                  >
                    {page}
                  </button>
                ))}

                <button
                  onClick={() => setCurrentPage((prev) => Math.min(totalPages, prev + 1))}
                  disabled={currentPage === totalPages}
                  className="p-2 rounded-lg hover:bg-gray-100 focus:outline-none focus:ring-2 focus:ring-[#FF6B01] disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                  style={{ color: '#353535' }}
                  aria-label={t('next')}
                >
                  <ChevronRight className="w-5 h-5" />
                </button>
              </div>
            </div>
          )}
        </>
      )}
        {/* Floating Action Button (Create New) */}
        <button
          onClick={() => onNavigate('/transactions/new')}
          aria-label={t('createNew')}
          className="fixed bottom-6 right-6 z-50 flex items-center gap-2 px-4 py-3 rounded-full shadow-lg hover:shadow-2xl focus:outline-none focus:ring-2 focus:ring-[#FF6B01] transition-transform"
          style={{ backgroundColor: '#FF6B01', color: '#FFFFFF' }}
        >
          <Plus className="w-5 h-5" />
          <span className="font-bold hidden sm:inline">{t('createNew')}</span>
        </button>
    </div>
  );
}