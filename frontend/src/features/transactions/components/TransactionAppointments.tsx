import { useState, useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { Calendar, Plus } from 'lucide-react';
import { Button } from "@/shared/components/ui/button";
import { Section } from "@/shared/components/branded/Section";
import { EmptyState } from "@/shared/components/branded/EmptyState";
import { LoadingState } from "@/shared/components/branded/LoadingState";
import { ErrorState } from "@/shared/components/branded/ErrorState";
import { useTransactionAppointments, useAppointments } from '@/features/appointments/api/queries';
import { AppointmentList } from '@/features/appointments/components/AppointmentList';
import { CreateAppointmentModal } from '@/features/appointments/components/CreateAppointmentModal';
import { type Appointment } from '@/features/appointments/types';
import { parseISO, format } from 'date-fns';

interface TransactionAppointmentsProps {
    transactionId: string;
    transactionAddress?: string;
    clientId?: string;
    isReadOnly?: boolean;
}

export function TransactionAppointments({
    transactionId,
    transactionAddress,
    clientId,
    isReadOnly = false
}: TransactionAppointmentsProps) {
    const { t } = useTranslation('appointments');
    const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);

    // Fetch transaction-specific appointments for display
    const {
        data: appointments = [],
        isLoading,
        error,
        refetch
    } = useTransactionAppointments(transactionId);

    // Fetch ALL appointments for accurate conflict checking
    const { data: allAppointments = [] } = useAppointments();

    // Group appointments by date
    const groupedAppointments = useMemo(() => {
        const groups = new Map<string, Appointment[]>();

        appointments.forEach((apt) => {
            // Assuming fromDateTime is in ISO format
            const dateKey = format(parseISO(apt.fromDateTime), 'yyyy-MM-dd');

            if (!groups.has(dateKey)) {
                groups.set(dateKey, []);
            }
            groups.get(dateKey)?.push(apt);
        });

        return groups;
    }, [appointments]);

    return (
        <Section>
            <div className="flex justify-between items-center mb-6">
                <h3 className="text-lg font-semibold">{t('title')}</h3>
                {!isReadOnly && (
                    <Button onClick={() => setIsCreateModalOpen(true)} size="sm">
                        <Plus className="w-4 h-4 mr-2" />
                        {t('scheduleAppointment')}
                    </Button>
                )}
            </div>

            {isLoading ? (
                <LoadingState />
            ) : error ? (
                <ErrorState message={error.message} onRetry={() => refetch()} />
            ) : (!appointments || appointments.length === 0) ? (
                <EmptyState
                    icon={<Calendar />}
                    title={t('noAppointments')}
                    description={t('appointmentsPlaceholder')}
                    action={
                        !isReadOnly ? (
                            <Button variant="outline" onClick={() => setIsCreateModalOpen(true)}>
                                {t('scheduleAppointment')}
                            </Button>
                        ) : undefined
                    }
                />
            ) : (
                <AppointmentList
                    groupedAppointments={groupedAppointments}
                    allAppointments={allAppointments}
                />
            )}

            <CreateAppointmentModal
                isOpen={isCreateModalOpen}
                onClose={() => setIsCreateModalOpen(false)}
                onSubmit={() => {
                    refetch();
                    setIsCreateModalOpen(false);
                }}
                fromTransaction={true}
                prefilledTransactionId={transactionId}
                prefilledTransactionAddress={transactionAddress}
                prefilledClientId={clientId}
                existingAppointments={allAppointments} // Use ALL appointments for conflict check here too
            />
        </Section>
    );
}
