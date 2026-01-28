import { useMemo } from 'react';
import { useClientAppointments } from '../api/useClientAppointments';
import { groupAppointmentsByDate } from '../types';

export function useClientAppointmentHistory(clientId: string) {
    const { data: appointments = [], isLoading, isError, refetch } = useClientAppointments(clientId);

    // Sort: upcoming first, then past
    const now = new Date();
    const upcoming = appointments.filter(a => new Date(a.fromDateTime) >= now);
    const past = appointments.filter(a => new Date(a.fromDateTime) < now);
    const sorted = [
        ...upcoming.sort((a, b) => new Date(a.fromDateTime).getTime() - new Date(b.fromDateTime).getTime()),
        ...past.sort((a, b) => new Date(b.fromDateTime).getTime() - new Date(a.fromDateTime).getTime()),
    ];

    const groupedAppointments = useMemo(() => groupAppointmentsByDate(sorted), [appointments]);

    return { appointments: sorted, groupedAppointments, isLoading, isError, refetch };
}
