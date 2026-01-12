import { useTranslation } from 'react-i18next';
import { Mail, Globe, User, FileText } from 'lucide-react';
import {
    Dialog,
    DialogContent,
    DialogHeader,
    DialogTitle,
    DialogDescription,
} from '@/shared/components/ui/dialog';
import { Avatar, AvatarFallback } from '@/shared/components/ui/avatar';
import { Separator } from '@/shared/components/ui/separator';
import { LoadingState } from '@/shared/components/branded/LoadingState';
import { ErrorState } from '@/shared/components/branded/ErrorState';
import { useAllClientTransactions } from '@/features/transactions/api/queries';
import { useUserProfile } from '@/features/profile';
import { ClientTransactionItem } from './ClientTransactionItem';
import { useClients } from '../hooks/useClients';
import type { Client } from '../api/clientsApi';

interface ClientDetailModalProps {
    client: Client | null;
    isOpen: boolean;
    onClose: () => void;
}

export function ClientDetailModal({ client: clientProp, isOpen, onClose }: ClientDetailModalProps) {
    const { t } = useTranslation('clients');
    const { data: allClients } = useClients();
    const { data: currentUser } = useUserProfile();

    // If the passed client has missing data (like email), look up the full client from the cache
    const client = clientProp?.id && allClients
        ? allClients.find(c => c.id === clientProp.id) ?? clientProp
        : clientProp;

    // Use the new hook that returns ALL transactions for a client (across all brokers)
    const { data: transactions, isLoading, isError, refetch } = useAllClientTransactions(client?.id ?? '');

    if (!client) return null;

    const initials = `${client.firstName?.[0] ?? ''}${client.lastName?.[0] ?? ''}`.toUpperCase();
    const hasName = client.firstName || client.lastName;
    const fullName = hasName
        ? `${client.firstName ?? ''} ${client.lastName ?? ''}`.trim()
        : client.email?.split('@')[0] ?? 'Unknown';

    const activeTransactions = transactions?.filter(tx => tx.status === 'ACTIVE') ?? [];
    const closedTransactions = transactions?.filter(tx => tx.status !== 'ACTIVE') ?? [];

    return (
        <Dialog open={isOpen} onOpenChange={(open) => !open && onClose()}>
            <DialogContent className="max-w-2xl max-h-[85vh] overflow-y-auto">
                <DialogHeader>
                    <div className="flex items-center gap-3">
                        <Avatar className="h-12 w-12">
                            <AvatarFallback className="bg-primary/10 text-primary font-medium">
                                {initials || <User className="h-5 w-5" />}
                            </AvatarFallback>
                        </Avatar>
                        <div>
                            <DialogTitle>{fullName}</DialogTitle>
                            <DialogDescription>{t('clientDetails')}</DialogDescription>
                        </div>
                    </div>
                </DialogHeader>

                <div className="space-y-6 mt-4">
                    {/* Contact Information */}
                    <section>
                        <h4 className="text-sm font-medium text-foreground mb-3 flex items-center gap-2">
                            <User className="h-4 w-4" />
                            {t('contactInfo')}
                        </h4>
                        <div className="space-y-2 pl-6">
                            <div className="flex items-center gap-2 text-sm">
                                <Mail className="h-4 w-4 text-muted-foreground" />
                                <span className="text-muted-foreground">{t('email')}:</span>
                                <span className="text-foreground">{client.email || <span className="italic text-muted-foreground">{t('emailNotAvailable')}</span>}</span>
                            </div>
                            <div className="flex items-center gap-2 text-sm">
                                <Globe className="h-4 w-4 text-muted-foreground" />
                                <span className="text-muted-foreground">{t('preferredLanguage')}:</span>
                                <span className="text-foreground capitalize">{client.preferredLanguage || '-'}</span>
                            </div>
                        </div>
                    </section>

                    <Separator />

                    {/* Transactions */}
                    <section>
                        <h4 className="text-sm font-medium text-foreground mb-3 flex items-center gap-2">
                            <FileText className="h-4 w-4" />
                            {t('transactions')}
                        </h4>

                        {isLoading && <LoadingState message={t('loadingTransactions')} />}

                        {isError && (
                            <ErrorState
                                message={t('errorLoadingTransactions')}
                                onRetry={() => refetch()}
                            />
                        )}

                        {!isLoading && !isError && transactions && (
                            <div className="space-y-4">
                                {/* Summary */}
                                <div className="flex items-center gap-4 text-sm pl-6">
                                    <span>
                                        <span className="font-medium text-green-600">{activeTransactions.length}</span>{' '}
                                        <span className="text-muted-foreground">{t('activeTransactions')}</span>
                                    </span>
                                    <span>
                                        <span className="font-medium text-muted-foreground">{closedTransactions.length}</span>{' '}
                                        <span className="text-muted-foreground">{t('closedTransactions')}</span>
                                    </span>
                                </div>

                                {/* Transaction List */}
                                {transactions.length === 0 ? (
                                    <p className="text-sm text-muted-foreground italic pl-6">{t('noTransactions')}</p>
                                ) : (
                                    <div className="space-y-2 pl-6">
                                        {transactions.map((transaction) => (
                                            <ClientTransactionItem
                                                key={transaction.transactionId}
                                                transaction={transaction}
                                                currentBrokerId={currentUser?.id}
                                            />
                                        ))}
                                    </div>
                                )}
                            </div>
                        )}
                    </section>
                </div>
            </DialogContent>
        </Dialog>
    );
}
