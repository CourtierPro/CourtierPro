import { useQuery } from '@tanstack/react-query';
import axiosInstance from '@/shared/api/axiosInstance';

export interface LoginAuditEvent {
    id: string;
    userId: string;
    email: string;
    role: string;
    timestamp: string;
    ipAddress?: string;
    userAgent?: string;
}

async function getLoginAuditEvents(): Promise<LoginAuditEvent[]> {
    const { data } = await axiosInstance.get<LoginAuditEvent[]>('/api/admin/login-audit');
    return data;
}

export function useLoginAudit() {
    return useQuery({
        queryKey: ['login-audit'],
        queryFn: getLoginAuditEvents,
    });
}
