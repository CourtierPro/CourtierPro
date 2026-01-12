import { useState, useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { Users, ArrowUpDown } from 'lucide-react';
import { PageHeader } from '@/shared/components/branded/PageHeader';
import { LoadingState } from '@/shared/components/branded/LoadingState';
import { ErrorState } from '@/shared/components/branded/ErrorState';
import { EmptyState } from '@/shared/components/branded/EmptyState';
import { Button } from '@/shared/components/ui/button';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuRadioGroup,
  DropdownMenuRadioItem,
  DropdownMenuTrigger,
} from '@/shared/components/ui/dropdown-menu';
import { useClients, type Client } from '@/features/clients';
import { useTransactions } from '@/features/transactions/api/queries';
import { ClientCard } from '@/features/clients/components/ClientCard';
import { ClientDetailModal } from '@/features/clients/components/ClientDetailModal';

type SortField = 'name' | 'email' | 'status';

export function ClientsPage() {
  const { t } = useTranslation('clients');
  const { data: clients, isLoading, isError, refetch } = useClients();
  const { data: transactions } = useTransactions();

  const [selectedClient, setSelectedClient] = useState<Client | null>(null);
  const [sortBy, setSortBy] = useState<SortField>('name');

  // Determine which clients have active transactions
  const clientActiveStatus = useMemo(() => {
    const statusMap = new Map<string, boolean>();
    if (clients && transactions) {
      clients.forEach(client => {
        const hasActive = transactions.some(
          tx => tx.clientId === client.id && tx.status === 'ACTIVE'
        );
        statusMap.set(client.id, hasActive);
      });
    }
    return statusMap;
  }, [clients, transactions]);

  // Sort clients based on selected field
  const sortedClients = useMemo(() => {
    if (!clients) return [];

    const sorted = [...clients];
    sorted.sort((a, b) => {
      switch (sortBy) {
        case 'name': {
          const nameA = `${a.firstName} ${a.lastName}`.toLowerCase();
          const nameB = `${b.firstName} ${b.lastName}`.toLowerCase();
          return nameA.localeCompare(nameB);
        }
        case 'email':
          return a.email.toLowerCase().localeCompare(b.email.toLowerCase());
        case 'status': {
          const activeA = clientActiveStatus.get(a.id) ? 1 : 0;
          const activeB = clientActiveStatus.get(b.id) ? 1 : 0;
          return activeB - activeA; // Active clients first
        }
        default:
          return 0;
      }
    });
    return sorted;
  }, [clients, sortBy, clientActiveStatus]);

  const handleClientClick = (client: Client) => {
    setSelectedClient(client);
  };

  const handleCloseModal = () => {
    setSelectedClient(null);
  };

  // Loading state
  if (isLoading) {
    return (
      <div className="space-y-6">
        <PageHeader title={t('title')} subtitle={t('subtitle')} />
        <LoadingState message={t('loading')} />
      </div>
    );
  }

  // Error state
  if (isError) {
    return (
      <div className="space-y-6">
        <PageHeader title={t('title')} subtitle={t('subtitle')} />
        <ErrorState
          title={t('error')}
          message={t('errorDescription')}
          onRetry={() => refetch()}
        />
      </div>
    );
  }

  // Empty state
  if (!clients || clients.length === 0) {
    return (
      <div className="space-y-6">
        <PageHeader title={t('title')} subtitle={t('subtitle')} />
        <EmptyState
          icon={<Users />}
          title={t('noClients')}
          description={t('noClientsDescription')}
        />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <PageHeader title={t('title')} subtitle={t('subtitle')} />

      {/* Sort Controls */}
      <div className="flex justify-end">
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button variant="outline" size="sm" className="gap-2">
              <ArrowUpDown className="h-4 w-4" />
              {t('sortBy')}: {t(`sortBy${sortBy.charAt(0).toUpperCase() + sortBy.slice(1)}` as 'sortByName' | 'sortByEmail' | 'sortByStatus')}
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end">
            <DropdownMenuRadioGroup value={sortBy} onValueChange={(v) => setSortBy(v as SortField)}>
              <DropdownMenuRadioItem value="name">{t('sortByName')}</DropdownMenuRadioItem>
              <DropdownMenuRadioItem value="email">{t('sortByEmail')}</DropdownMenuRadioItem>
              <DropdownMenuRadioItem value="status">{t('sortByStatus')}</DropdownMenuRadioItem>
            </DropdownMenuRadioGroup>
          </DropdownMenuContent>
        </DropdownMenu>
      </div>

      {/* Client Cards Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {sortedClients.map((client) => (
          <ClientCard
            key={client.id}
            client={client}
            hasActiveTransaction={clientActiveStatus.get(client.id) ?? false}
            onClick={() => handleClientClick(client)}
          />
        ))}
      </div>

      {/* Client Detail Modal */}
      <ClientDetailModal
        client={selectedClient}
        isOpen={!!selectedClient}
        onClose={handleCloseModal}
      />
    </div>
  );
}
