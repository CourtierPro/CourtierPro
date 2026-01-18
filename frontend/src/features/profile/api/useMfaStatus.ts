import { useQuery } from '@tanstack/react-query';
import axiosInstance from '@/shared/api/axiosInstance';

export function useMfaStatus() {
    return useQuery({
        queryKey: ['profile', 'mfa-status'],
        queryFn: async () => {
            const response = await axiosInstance.get<{ mfaEnabled: boolean }>('/api/me/mfa-status');
            return response.data.mfaEnabled;
        },
    });
}
