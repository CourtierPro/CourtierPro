import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import axiosInstance from '@/shared/api/axiosInstance';

export interface NotificationResponseDTO {
    publicId: string;
    title: string;
    message: string;
    isRead: boolean;
    relatedTransactionId: string | null;
    createdAt: string; // ISO timestamp
}

const fetchNotifications = async (): Promise<NotificationResponseDTO[]> => {
    const { data } = await axiosInstance.get<NotificationResponseDTO[]>('/api/v1/notifications');
    return data;
};

const markNotificationAsRead = async (publicId: string): Promise<void> => {
    await axiosInstance.put<void>(`/api/v1/notifications/${publicId}/read`);
};

export const useNotifications = () => {
    return useQuery({
        queryKey: ['notifications'],
        queryFn: fetchNotifications,
    });
};

export const useMarkNotificationAsRead = () => {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: markNotificationAsRead,
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['notifications'] });
        },
    });
};
