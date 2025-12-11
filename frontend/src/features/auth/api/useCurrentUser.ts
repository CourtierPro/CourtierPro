import { useQuery } from '@tanstack/react-query';
import axiosInstance from '@/shared/api/axiosInstance';

export interface CurrentUserProfile {
    id: string;  // Internal UUID
    email: string;
    firstName: string;
    lastName: string;
    role: string;
    active: boolean;
    preferredLanguage: string;
}

export const currentUserKeys = {
    profile: ['currentUser', 'profile'] as const,
};

/**
 * Fetches the current user's profile from the /api/me endpoint.
 * The response includes the internal UUID which should be used for all API calls.
 */
export function useCurrentUser() {
    return useQuery({
        queryKey: currentUserKeys.profile,
        queryFn: async (): Promise<CurrentUserProfile> => {
            const response = await axiosInstance.get<CurrentUserProfile>('/api/me');
            return response.data;
        },
        staleTime: 5 * 60 * 1000, // Cache for 5 minutes
        retry: 1,
    });
}
