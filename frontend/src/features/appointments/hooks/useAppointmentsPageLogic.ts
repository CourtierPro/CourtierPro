import { useState } from 'react';
import { useAppointmentsForMonth } from '../api/queries';
import { type AppointmentViewMode, groupAppointmentsByDate } from '../types';
import { useMemo } from 'react';

export function useAppointmentsPageLogic() {
    // View state
    const [viewMode, setViewMode] = useState<AppointmentViewMode>('calendar');
    const [currentDate, setCurrentDate] = useState(new Date());

    // Fetch appointments for current month
    const currentYear = currentDate.getFullYear();
    const currentMonth = currentDate.getMonth();

    const { data: appointments = [], isLoading, error, refetch } = useAppointmentsForMonth(
        currentYear,
        currentMonth
    );

    // Group appointments by date for list view
    const groupedAppointments = useMemo(() => {
        return groupAppointmentsByDate(appointments);
    }, [appointments]);

    // Date navigation
    const goToPreviousMonth = () => {
        setCurrentDate(new Date(currentYear, currentMonth - 1, 1));
    };

    const goToNextMonth = () => {
        setCurrentDate(new Date(currentYear, currentMonth + 1, 1));
    };

    const goToToday = () => {
        setCurrentDate(new Date());
    };

    return {
        // Data
        appointments,
        groupedAppointments,
        isLoading,
        error,
        refetch,

        // View state
        viewMode,
        setViewMode,
        currentDate,
        currentMonth,
        currentYear,

        // Date navigation
        goToPreviousMonth,
        goToNextMonth,
        goToToday,
    };
}
