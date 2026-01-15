import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import axiosInstance from '@/shared/api/axiosInstance';

export const NotificationType = {
    GENERAL: 'GENERAL',
    BROADCAST: 'BROADCAST',
} as const;

export type NotificationType = (typeof NotificationType)[keyof typeof NotificationType];

export const NotificationCategory = {
    WELCOME: 'WELCOME',
    DOCUMENT_REQUEST: 'DOCUMENT_REQUEST',
    DOCUMENT_SUBMITTED: 'DOCUMENT_SUBMITTED',
    DOCUMENT_APPROVED: 'DOCUMENT_APPROVED',
    DOCUMENT_REVISION: 'DOCUMENT_REVISION',
    DOCUMENT_REJECTED: 'DOCUMENT_REJECTED',
    STAGE_UPDATE: 'STAGE_UPDATE',
    TRANSACTION_CANCELLED: 'TRANSACTION_CANCELLED',
    PROPERTY_ADDED: 'PROPERTY_ADDED',
    OFFER_RECEIVED: 'OFFER_RECEIVED',
    OFFER_MADE: 'OFFER_MADE',
    OFFER_STATUS_CHANGED: 'OFFER_STATUS_CHANGED',
    OFFER_COUNTERED: 'OFFER_COUNTERED',
    CONDITION_ADDED: 'CONDITION_ADDED',
    CONDITION_STATUS_CHANGED: 'CONDITION_STATUS_CHANGED',
    BROADCAST: 'BROADCAST',
    GENERAL: 'GENERAL',
} as const;

export type NotificationCategory = (typeof NotificationCategory)[keyof typeof NotificationCategory];

export interface NotificationResponseDTO {
    publicId: string;
    title: string;
    message: string;
    titleKey?: string;
    messageKey?: string;
    params?: string; // JSON string with parameters
    read: boolean;
    relatedTransactionId: string | null;
    type: NotificationType;
    category: NotificationCategory;
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
        refetchInterval: () =>
            typeof document !== 'undefined' && document.visibilityState === 'visible'
                ? 60000 // 60 seconds
                : false,
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
