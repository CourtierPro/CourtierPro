import { useMutation, useQueryClient } from '@tanstack/react-query';
import axiosInstance from '@/shared/api/axiosInstance';
import { profileKeys, type UserProfile } from '@/features/profile/api/queries';

export interface UpdateProfileData {
    preferredLanguage?: string;
    email?: string;
    emailNotificationsEnabled?: boolean;
    inAppNotificationsEnabled?: boolean;
    weeklyDigestEnabled?: boolean;
}

export function useUpdateUserProfile() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async (data: UpdateProfileData) => {
            const response = await axiosInstance.patch<UserProfile>('/api/me', data);
            return response.data;
        },
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: profileKeys.details() });
        },
    });
}
