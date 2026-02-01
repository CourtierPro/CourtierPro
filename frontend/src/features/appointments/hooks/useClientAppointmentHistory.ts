import { useMemo } from 'react';
import { useClientAppointments } from '../api/useClientAppointments';
import { groupAppointmentsByDate } from '../types';

export function useClientAppointmentHistory(clientId: string) {
    const { data: appointments = [], isLoading, isError, refetch } = useClientAppointments(clientId);

    // Sort: upcoming first, then past
    const sorted = useMemo(() => {
        const now = new Date();
        const upcoming = appointments.filter(a => new Date(a.fromDateTime) >= now);
        const past = appointments.filter(a => new Date(a.fromDateTime) < now);

        return [
            ...upcoming.sort((a, b) => new Date(a.fromDateTime).getTime() - new Date(b.fromDateTime).getTime()),
            ...past.sort((a, b) => new Date(b.fromDateTime).getTime() - new Date(a.fromDateTime).getTime()),
        ];
    }, [appointments]);

    const groupedAppointments = useMemo(() => groupAppointmentsByDate(sorted), [sorted]);

    return { appointments: sorted, groupedAppointments, isLoading, isError, refetch };
}
