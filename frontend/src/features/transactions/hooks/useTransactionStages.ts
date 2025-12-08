import { useQuery } from '@tanstack/react-query';
import { axiosInstance } from '@/shared/api/axiosInstance';

interface StagesResponse {
    BUYER_STAGES: string[];
    SELLER_STAGES: string[];
}

export const useTransactionStages = (side?: 'BUY_SIDE' | 'SELL_SIDE') => {
    const { data, isLoading, error } = useQuery({
        queryKey: ['transactionStages'],
        queryFn: async () => {
            const response = await axiosInstance.get<StagesResponse>('/transactions/stages');
            return response.data;
        },
        staleTime: 1000 * 60 * 60, // Cache for 1 hour (enums rarely change)
    });

    if (!side) {
        return {
            stages: [],
            allStages: data,
            isLoading,
            error
        };
    }

    const stages = side === 'BUY_SIDE'
        ? data?.BUYER_STAGES
        : data?.SELLER_STAGES;

    return {
        stages: stages || [],
        isLoading,
        error
    };
};
