import { useQuery } from '@tanstack/react-query';

export const profileKeys = {
    all: ['profile'] as const,
    details: () => [...profileKeys.all, 'detail'] as const,
};

export interface UserProfile {
    id: string;
    name: string;
    email: string;
    role: string;
    agency: string;
    licenseNumber: string;
}

export function useUserProfile() {
    return useQuery({
        queryKey: profileKeys.details(),
        queryFn: async () => {
            // In a real app: const res = await axiosInstance.get<UserProfile>('/profile');
            // For now, return mock data
            await new Promise((resolve) => setTimeout(resolve, 500));
            return {
                id: 'u1',
                name: "John Doe",
                email: "john.doe@example.com",
                role: "Broker",
                agency: "CourtierPro Realty",
                licenseNumber: "BR-123456"
            } as UserProfile;
        },
    });
}
