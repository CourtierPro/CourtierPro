import axiosInstance from '@/shared/api/axiosInstance';

export interface LogoutEventPayload {
  reason: 'manual' | 'session_timeout' | 'forced';
  timestamp: string;
}

/**
 * Logs a logout event to the backend
 * 
 * @param payload - The logout event data
 * @returns Promise that resolves when the event is logged
 */
export async function logLogoutEvent(payload: LogoutEventPayload): Promise<void> {
  await axiosInstance.post('/auth/logout', payload);
}
