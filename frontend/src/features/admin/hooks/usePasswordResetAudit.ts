import { useQuery } from '@tanstack/react-query';
import axiosInstance from '@/shared/api/axiosInstance';

export interface PasswordResetEvent {
    id: string;
    userId: string;
    email: string;
    eventType: 'REQUESTED' | 'COMPLETED';
    timestamp: string;
    ipAddress?: string;
    userAgent?: string;
}

async function getPasswordResetAudit(): Promise<PasswordResetEvent[]> {
    const { data } = await axiosInstance.get<PasswordResetEvent[]>('/api/admin/password-reset-audit');
    return data;
}

export function usePasswordResetAudit() {
    return useQuery({
        queryKey: ['password-reset-audit'],
        queryFn: getPasswordResetAudit,
    });
}
