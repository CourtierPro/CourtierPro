import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import axiosInstance from '@/shared/api/axiosInstance';

export const NotificationType = {
    GENERAL: 'GENERAL',
    BROADCAST: 'BROADCAST',
} as const;

export type NotificationType = (typeof NotificationType)[keyof typeof NotificationType];

export interface NotificationResponseDTO {
    publicId: string;
    title: string;
    message: string;
    read: boolean;
    relatedTransactionId: string | null;
    type: NotificationType;
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

const sendBroadcast = async (data: { title: string; message: string }): Promise<void> => {
    await axiosInstance.post<void>('/api/v1/notifications/broadcast', data);
};

export const useSendBroadcast = () => {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: sendBroadcast,
        onSuccess: () => {
            // Optionally invalidate notifications if the sender should also receive it immediately,
            // though typically broadcasts might take a moment or depend on logic.
            queryClient.invalidateQueries({ queryKey: ['notifications'] });
        },
    });
};
