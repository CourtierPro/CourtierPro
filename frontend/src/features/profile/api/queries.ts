import { useQuery } from '@tanstack/react-query';
import axiosInstance from '@/shared/api/axiosInstance';

export const profileKeys = {
    all: ['profile'] as const,
    details: () => [...profileKeys.all, 'detail'] as const,
};

export interface UserProfile {
    id: string;
    email: string;
    firstName: string;
    lastName: string;
    role: 'BROKER' | 'CLIENT' | 'ADMIN';
    active: boolean;
    preferredLanguage: string;
}

export function useUserProfile() {
    return useQuery({
        queryKey: profileKeys.details(),
        queryFn: async () => {
            const response = await axiosInstance.get<UserProfile>('/api/me');
            return response.data;
        },
    });
}
