import { useTranslation } from 'react-i18next';
import { formatDateTime } from '@/shared/utils/date';
import { TimelineEntry } from '@/shared/api/types';

interface AppointmentTimelineItemProps {
    entry: TimelineEntry;
}

export function AppointmentTimelineItem({ entry }: AppointmentTimelineItemProps) {
    const { t } = useTranslation('transactions');

    if (!entry.transactionInfo) return null;

    const { appointmentTitle, appointmentDate } = entry.transactionInfo;
    const dateStr = appointmentDate ? formatDateTime(appointmentDate) : '';

    // Translate title if it matches a known key
    const translatedTitle = appointmentTitle
        ? t(`appointmentTypes.${appointmentTitle.toLowerCase()}`, { defaultValue: appointmentTitle })
        : '';

    if (entry.type === 'APPOINTMENT_CONFIRMED') {
        return <p>{t('timeline.appointmentConfirmedDetail', { title: translatedTitle, date: dateStr })}</p>;
    }
    if (entry.type === 'APPOINTMENT_CANCELLED') {
        return (
            <div className="mt-1">
                <p className="mb-2">{t('timeline.appointmentCancelledDetail', { title: translatedTitle })}</p>
                {entry.note && (
                    <div className="bg-red-50 px-3 py-2 rounded-md text-sm text-red-900 border border-red-200 inline-block">
                        <span className="font-semibold mr-1">{t('reasonLabel')}:</span>
                        {entry.note}
                    </div>
                )}
            </div>
        );
    }
    if (entry.type === 'APPOINTMENT_DECLINED') {
        return (
            <div className="mt-1">
                <p className="mb-2">{t('timeline.appointmentDeclinedDetail', { title: translatedTitle })}</p>
                {entry.note && (
                    <div className="bg-red-50 px-3 py-2 rounded-md text-sm text-red-900 border border-red-200 inline-block">
                        <span className="font-semibold mr-1">{t('reasonLabel')}:</span>
                        {entry.note}
                    </div>
                )}
            </div>
        );
    }
    if (entry.type === 'APPOINTMENT_RESCHEDULED') {
        return <p>{t('timeline.appointmentRescheduledDetail', { title: translatedTitle, date: dateStr })}</p>;
    }
    if (entry.type === 'APPOINTMENT_REQUESTED') {
        return <p>{t('timeline.appointmentRequestedDetail', { title: translatedTitle, date: dateStr })}</p>;
    }
    return null;
}
