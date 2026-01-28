import { useQuery } from '@tanstack/react-query';
import axiosInstance from '@/shared/api/axiosInstance';
import { type Appointment } from '../types';

export function useClientAppointments(clientId: string) {
    return useQuery({
        queryKey: ['appointments', 'client', clientId],
        queryFn: async () => {
            const res = await axiosInstance.get<Appointment[]>(`/appointments/client/${encodeURIComponent(clientId)}`);
            return res.data;
        },
        enabled: !!clientId,
    });
}
