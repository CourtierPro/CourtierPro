import { useMutation, useQueryClient } from '@tanstack/react-query';
import { appointmentKeys, type Appointment } from '@/features/appointments/api/queries';

export interface CreateAppointmentDTO {
    type: string;
    date: string;
    time: string;
    message: string;
    clientId: string;
    transactionId: string;
}

export function useCreateAppointment() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async (newAppointment: CreateAppointmentDTO) => {
            // In a real app: const res = await axiosInstance.post('/appointments', newAppointment);
            await new Promise((resolve) => setTimeout(resolve, 1000));
            return { ...newAppointment, id: 'temp-id', status: 'pending' } as Appointment;
        },
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: appointmentKeys.lists() });
        },
    });
}
