import { useQuery } from '@tanstack/react-query';
import axiosInstance from '@/shared/api/axiosInstance';

export const appointmentKeys = {
    all: ['appointments'] as const,
    lists: () => [...appointmentKeys.all, 'list'] as const,
    details: () => [...appointmentKeys.all, 'detail'] as const,
    detail: (id: string) => [...appointmentKeys.details(), id] as const,
};

export interface Appointment {
    id: string;
    type: 'inspection' | 'notary' | 'showing' | 'consultation' | 'walkthrough' | 'meeting';
    date: string;
    time: string;
    message?: string;
    clientId: string;
    clientName: string;
    transactionId: string;
    transactionAddress: string;
    status: 'pending' | 'confirmed' | 'cancelled';
}

export function useAppointments() {
    return useQuery({
        queryKey: appointmentKeys.lists(),
        queryFn: async () => {
            // In a real app: const res = await axiosInstance.get<Appointment[]>('/appointments');
            // For now, return mock data or empty array if endpoint doesn't exist
            try {
                const res = await axiosInstance.get<Appointment[]>('/appointments');
                return res.data;
            } catch (error) {
                console.warn('Failed to fetch appointments, returning empty list', error);
                return [];
            }
        },
    });
}
