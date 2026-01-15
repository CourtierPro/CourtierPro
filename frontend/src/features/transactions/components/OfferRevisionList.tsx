import { useTranslation } from 'react-i18next';
import { format } from 'date-fns';
import { useOfferRevisions } from '@/features/transactions/api/queries';
import { LoadingState } from '@/shared/components/branded/LoadingState';
import { ErrorState } from '@/shared/components/branded/ErrorState';
import { Badge } from '@/shared/components/ui/badge';
import { ScrollArea } from '@/shared/components/ui/scroll-area';
import { History } from 'lucide-react';

interface OfferRevisionListProps {
    transactionId: string;
    offerId: string;
}

export function OfferRevisionList({ transactionId, offerId }: OfferRevisionListProps) {
    const { t } = useTranslation('transactions');
    const { data: revisions, isLoading, error } = useOfferRevisions(transactionId, offerId);

    const formatCurrency = (amount?: number) => {
        if (!amount) return '-';
        return new Intl.NumberFormat('en-CA', {
            style: 'currency',
            currency: 'CAD',
            maximumFractionDigits: 0,
        }).format(amount);
    };

    const formatDate = (dateString: string) => {
        return format(new Date(dateString), 'MMM d, yyyy h:mm a');
    };

    if (isLoading) {
        return <LoadingState message={t('loadingRevisions')} />;
    }

    if (error) {
        return <ErrorState message={t('errorLoadingRevisions')} />;
    }

    if (!revisions || revisions.length === 0) {
        return (
            <div className="text-center py-8 text-muted-foreground border-2 border-dashed rounded-lg">
                <History className="w-8 h-8 mx-auto mb-2 opacity-50" />
                <p>{t('noRevisions')}</p>
            </div>
        );
    }

    // Sort by revision number descending
    const sortedRevisions = [...revisions].sort((a, b) => b.revisionNumber - a.revisionNumber);

    return (
        <ScrollArea className="h-[400px] pr-4">
            <div className="space-y-4">
                {sortedRevisions.map((rev) => (
                    <div key={rev.revisionId} className="border rounded-lg p-3 space-y-2 bg-card">
                        <div className="flex justify-between items-center text-sm text-muted-foreground">
                            <span>{t('revisionNumber', { number: rev.revisionNumber })}</span>
                            <span>{formatDate(rev.createdAt)}</span>
                        </div>

                        <div className="space-y-1">
                            {rev.newAmount && rev.newAmount !== rev.previousAmount && (
                                <div className="flex justify-between items-center">
                                    <span className="text-sm font-medium">{t('amount')}</span>
                                    <div className="flex items-center gap-2 text-sm">
                                        <span className="text-muted-foreground line-through">
                                            {formatCurrency(rev.previousAmount)}
                                        </span>
                                        <span>→</span>
                                        <span className="font-semibold">{formatCurrency(rev.newAmount)}</span>
                                    </div>
                                </div>
                            )}

                            {rev.newStatus && rev.newStatus !== rev.previousStatus && (
                                <div className="flex justify-between items-center">
                                    <span className="text-sm font-medium">{t('status')}</span>
                                    <div className="flex items-center gap-2 text-sm">
                                        <Badge variant="outline" className="text-muted-foreground">
                                            {rev.previousStatus ? t(`receivedOfferStatuses.${rev.previousStatus}`) : '-'}
                                        </Badge>
                                        <span>→</span>
                                        <Badge variant="secondary">
                                            {t(`receivedOfferStatuses.${rev.newStatus}`)}
                                        </Badge>
                                    </div>
                                </div>
                            )}
                        </div>
                    </div>
                ))}
            </div>
        </ScrollArea>
    );
}
