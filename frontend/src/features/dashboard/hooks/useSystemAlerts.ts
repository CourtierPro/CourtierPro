import { useQuery } from '@tanstack/react-query';
import { axiosInstance } from '@/shared/api/axiosInstance';

export interface SystemAlert {
  id: number;
  message: string;
  severity: string;
  active: boolean;
  createdAt: string;
}

export function useSystemAlerts() {
  return useQuery<SystemAlert[]>({
    queryKey: ['system-alerts'],
    queryFn: async () => {
      const res = await axiosInstance.get<SystemAlert[]>('/api/v1/dashboard/admin/alerts');
      return res.data;
    },
  });
}
