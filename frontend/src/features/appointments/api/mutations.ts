import { useMutation, useQueryClient } from '@tanstack/react-query';
import axiosInstance from '@/shared/api/axiosInstance';
import { appointmentKeys } from './queries';

export interface AppointmentRequestDTO {
    transactionId: string;
    type: string;
    date: string; // ISO date YYYY-MM-DD
    time: string; // ISO time HH:mm
    message: string;
}

export function useRequestAppointment() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async (data: AppointmentRequestDTO) => {
            const res = await axiosInstance.post('/appointments', data);
            return res.data;
        },
        onSuccess: (_, variables) => {
            // Invalidate relevant queries
            queryClient.invalidateQueries({ queryKey: appointmentKeys.all });
            queryClient.invalidateQueries({ queryKey: ['transactions', 'detail', variables.transactionId] });
        },
    });
}
