import { useQuery } from '@tanstack/react-query';
import axiosInstance from '@/shared/api/axiosInstance';

export const documentKeys = {
    all: ['documents'] as const,
    lists: () => [...documentKeys.all, 'list'] as const,
    details: () => [...documentKeys.all, 'detail'] as const,
    detail: (id: string) => [...documentKeys.details(), id] as const,
};

export interface Document {
    id: string;
    title: string;
    type: string;
    url: string;
    uploadedAt: string;
    transactionId?: string;
    status: 'pending' | 'signed' | 'uploaded';
}

export function useDocuments() {
    return useQuery({
        queryKey: documentKeys.lists(),
        queryFn: async () => {
            // In a real app: const res = await axiosInstance.get<Document[]>('/documents');
            // For now, return mock data or empty array
            try {
                const res = await axiosInstance.get<Document[]>('/documents');
                return res.data;
            } catch (error) {
                console.warn('Failed to fetch documents, returning empty list', error);
                return [];
            }
        },
    });
}
