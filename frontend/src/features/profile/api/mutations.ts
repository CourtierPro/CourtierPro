import { useMutation, useQueryClient } from '@tanstack/react-query';
import { profileKeys, type UserProfile } from '@/features/profile/api/queries';

export function useUpdateUserProfile() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async (data: Partial<UserProfile>) => {
            // In a real app: const res = await axiosInstance.patch('/profile', data);
            await new Promise((resolve) => setTimeout(resolve, 1000));
            return data;
        },
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: profileKeys.details() });
        },
    });
}
