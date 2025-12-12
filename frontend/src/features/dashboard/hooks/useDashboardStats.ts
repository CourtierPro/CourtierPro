import { useQuery } from '@tanstack/react-query';
import { axiosInstance } from '@/shared/api/axiosInstance';

export interface ClientDashboardStats {
    activeTransactions: number;
    documentsNeeded: number;
}

export interface BrokerDashboardStats {
    activeTransactions: number;
    activeClients: number;
    totalCommission: number;
}

export interface AdminDashboardStats {
    totalUsers: number;
    activeBrokers: number;
    systemHealth: string;
}

export function useClientDashboardStats() {
    return useQuery({
        queryKey: ['dashboard', 'client'],
        queryFn: async () => {
            const res = await axiosInstance.get<ClientDashboardStats>('/api/v1/dashboard/client');
            return res.data;
        },
    });
}

export function useBrokerDashboardStats() {
    return useQuery({
        queryKey: ['dashboard', 'broker'],
        queryFn: async () => {
            const res = await axiosInstance.get<BrokerDashboardStats>('/api/v1/dashboard/broker');
            return res.data;
        },
    });
}

export function useAdminDashboardStats() {
    return useQuery({
        queryKey: ['dashboard', 'admin'],
        queryFn: async () => {
            const res = await axiosInstance.get<AdminDashboardStats>('/api/v1/dashboard/admin');
            return res.data;
        },
    });
}
