import { useMutation, useQueryClient } from '@tanstack/react-query';
import axiosInstance from '@/shared/api/axiosInstance';
import { appointmentKeys } from './queries';

export interface AppointmentRequestDTO {
    transactionId: string;
    type: string;
    title?: string; // Custom title for 'other' type
    date: string; // ISO date YYYY-MM-DD
    startTime: string; // ISO time HH:mm
    endTime: string; // ISO time HH:mm
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
export interface AppointmentReviewDTO {
    action: 'CONFIRM' | 'DECLINE' | 'RESCHEDULE';
    refusalReason?: string;
    newDate?: string; // YYYY-MM-DD
    newStartTime?: string; // HH:mm
    newEndTime?: string; // HH:mm
}

export function useReviewAppointment() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async ({ id, data }: { id: string; data: AppointmentReviewDTO }) => {
            const res = await axiosInstance.patch(`/appointments/${id}/review`, data);
            return res.data;
        },
        onSuccess: (data) => {
            queryClient.invalidateQueries({ queryKey: appointmentKeys.all });
            if (data.transactionId) {
                queryClient.invalidateQueries({ queryKey: ['transactions', 'detail', data.transactionId, 'timeline'] });
            }
        },
    });
}

export interface AppointmentCancellationDTO {
    reason: string;
}

export function useCancelAppointment() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async ({ id, data }: { id: string; data: AppointmentCancellationDTO }) => {
            const res = await axiosInstance.patch(`/appointments/${id}/cancel`, data);
            return res.data;
        },
        onSuccess: (data) => {
            queryClient.invalidateQueries({ queryKey: appointmentKeys.all });
            if (data.transactionId) {
                queryClient.invalidateQueries({ queryKey: ['transactions', 'detail', data.transactionId, 'timeline'] });
            }
        },
    });
}
