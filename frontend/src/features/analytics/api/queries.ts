import { useQuery } from '@tanstack/react-query';
import axiosInstance from '@/shared/api/axiosInstance';
import { type AnalyticsData } from '../types';

export const analyticsKeys = {
    all: ['analytics'] as const,
    summary: () => [...analyticsKeys.all, 'summary'] as const,
};

export function useAnalytics() {
    return useQuery({
        queryKey: analyticsKeys.summary(),
        queryFn: async () => {
            const res = await axiosInstance.get<AnalyticsData>('/analytics');
            return res.data;
        },
        staleTime: 30 * 1000,
    });
}
