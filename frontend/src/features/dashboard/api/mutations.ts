import { useMutation, useQueryClient } from '@tanstack/react-query';
import { axiosInstance } from '@/shared/api/axiosInstance';
import { dashboardKeys } from './queries';

interface MarkActivitiesSeenRequest {
    activityIds: string[];
}

interface MarkActivitiesSeenResponse {
    success: boolean;
    markedCount: number;
}

/**
 * Mutation to mark specific activities as seen.
 * Used when clicking on an activity or selecting multiple to mark as seen.
 */
export function useMarkActivitiesAsSeen() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async (activityIds: string[]): Promise<MarkActivitiesSeenResponse> => {
            const res = await axiosInstance.post<MarkActivitiesSeenResponse>(
                '/api/v1/dashboard/broker/recent-activity/mark-seen',
                { activityIds } as MarkActivitiesSeenRequest
            );
            return res.data;
        },
        onSuccess: () => {
            // Invalidate recent activity queries to refetch with updated seen status
            queryClient.invalidateQueries({ queryKey: dashboardKeys.all });
        },
    });
}
