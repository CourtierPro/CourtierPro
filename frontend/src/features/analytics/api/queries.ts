import { useQuery } from '@tanstack/react-query';
import axiosInstance from '@/shared/api/axiosInstance';
import { type AnalyticsData, type AnalyticsFilter } from '../types';

export const analyticsKeys = {
    all: ['analytics'] as const,
    summary: (filters?: AnalyticsFilter) => [...analyticsKeys.all, 'summary', filters] as const,
};

export function useAnalytics(filters?: AnalyticsFilter) {
    return useQuery({
        queryKey: analyticsKeys.summary(filters),
        queryFn: async () => {
            const params = new URLSearchParams();
            if (filters?.startDate) params.append('startDate', filters.startDate);
            if (filters?.endDate) params.append('endDate', filters.endDate);
            if (filters?.transactionType) params.append('transactionType', filters.transactionType);
            if (filters?.clientName) params.append('clientName', filters.clientName);

            const res = await axiosInstance.get<AnalyticsData>('/analytics', { params });
            return res.data;
        },
        staleTime: 30 * 1000,
    });
}

export const exportAnalyticsCsv = async (filters?: AnalyticsFilter) => {
    const params = new URLSearchParams();
    if (filters?.startDate) params.append('startDate', filters.startDate);
    if (filters?.endDate) params.append('endDate', filters.endDate);
    if (filters?.transactionType) params.append('transactionType', filters.transactionType);
    if (filters?.clientName) params.append('clientName', filters.clientName);

    const response = await axiosInstance.get('/analytics/export/csv', {
        params,
        responseType: 'blob',
    });

    const url = window.URL.createObjectURL(new Blob([response.data]));
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', `analytics_export_${new Date().toISOString().split('T')[0]}.csv`);
    document.body.appendChild(link);
    link.click();
    link.remove();
};

export const exportAnalyticsPdf = async (filters?: AnalyticsFilter) => {
    const params = new URLSearchParams();
    if (filters?.startDate) params.append('startDate', filters.startDate);
    if (filters?.endDate) params.append('endDate', filters.endDate);
    if (filters?.transactionType) params.append('transactionType', filters.transactionType);
    if (filters?.clientName) params.append('clientName', filters.clientName);

    const response = await axiosInstance.get('/analytics/export/pdf', {
        params,
        responseType: 'blob',
    });

    const url = window.URL.createObjectURL(new Blob([response.data]));
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', `analytics_export_${new Date().toISOString().split('T')[0]}.pdf`);
    document.body.appendChild(link);
    link.click();
    link.remove();
};
