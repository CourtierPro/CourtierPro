import { useQuery, keepPreviousData } from '@tanstack/react-query';
import axiosInstance from '@/shared/api/axiosInstance';
import { type Appointment, type AppointmentFilters } from '../types';

export const appointmentKeys = {
    all: ['appointments'] as const,
    lists: () => [...appointmentKeys.all, 'list'] as const,
    listFiltered: (filters: AppointmentFilters) => [...appointmentKeys.lists(), filters] as const,
    details: () => [...appointmentKeys.all, 'detail'] as const,
    detail: (id: string) => [...appointmentKeys.details(), id] as const,
    byTransaction: (transactionId: string) => [...appointmentKeys.all, 'transaction', transactionId] as const,
};

/**
 * Fetch all appointments for the current user with optional filters.
 */
export function useAppointments(filters?: AppointmentFilters) {
    return useQuery({
        queryKey: filters ? appointmentKeys.listFiltered(filters) : appointmentKeys.lists(),
        queryFn: async () => {
            const params = new URLSearchParams();
            if (filters?.from) params.append('from', filters.from);
            if (filters?.to) params.append('to', filters.to);
            if (filters?.status) params.append('status', filters.status);

            const queryString = params.toString();
            const url = queryString ? `/appointments?${queryString}` : '/appointments';

            const res = await axiosInstance.get<Appointment[]>(url);
            return res.data;
        },
        placeholderData: keepPreviousData,
    });
}

/**
 * Fetch a single appointment by ID.
 */
export function useAppointment(appointmentId: string | undefined) {
    return useQuery({
        queryKey: appointmentKeys.detail(appointmentId ?? ''),
        queryFn: async () => {
            const res = await axiosInstance.get<Appointment>(`/appointments/${appointmentId}`);
            return res.data;
        },
        enabled: !!appointmentId,
    });
}

/**
 * Fetch appointments for a specific transaction.
 */
export function useTransactionAppointments(transactionId: string | undefined) {
    return useQuery({
        queryKey: appointmentKeys.byTransaction(transactionId ?? ''),
        queryFn: async () => {
            const res = await axiosInstance.get<Appointment[]>(`/appointments/transaction/${transactionId}`);
            return res.data;
        },
        enabled: !!transactionId,
    });
}

/**
 * Fetch appointments for a specific month (for calendar view).
 */
export function useAppointmentsForMonth(year: number, month: number) {
    // month is 0-indexed (0 = January)
    const from = new Date(year, month, 1).toISOString();
    const to = new Date(year, month + 1, 1).toISOString();

    return useAppointments({ from, to });
}
