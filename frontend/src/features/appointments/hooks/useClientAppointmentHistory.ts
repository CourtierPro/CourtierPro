import { useMemo } from 'react';
import { useClientAppointments } from '../api/useClientAppointments';
import { groupAppointmentsByDate } from '../types';

export function useClientAppointmentHistory(clientId: string) {
    const { data: appointments = [], isLoading, isError, refetch } = useClientAppointments(clientId);

    // Sort: upcoming first, then past - wrapped in useMemo to avoid dependency issues
    const { sorted, groupedAppointments } = useMemo(() => {
        const now = new Date();
        const upcoming = appointments.filter(a => new Date(a.fromDateTime) >= now);
        const past = appointments.filter(a => new Date(a.fromDateTime) < now);
        const sortedAppointments = [
            ...upcoming.sort((a, b) => new Date(a.fromDateTime).getTime() - new Date(b.fromDateTime).getTime()),
            ...past.sort((a, b) => new Date(b.fromDateTime).getTime() - new Date(a.fromDateTime).getTime()),
        ];
        return {
            sorted: sortedAppointments,
            groupedAppointments: groupAppointmentsByDate(sortedAppointments),
        };
    }, [appointments]);

    return { appointments: sorted, groupedAppointments, isLoading, isError, refetch };
}
