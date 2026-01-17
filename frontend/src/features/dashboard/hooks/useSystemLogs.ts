import { useQuery } from '@tanstack/react-query';
import { axiosInstance } from '@/shared/api/axiosInstance';

export interface OrgSettingsLogEntry {
  id: string;
  timestamp: string | null;
  adminUserId: string | null;
  adminEmail: string | null;
  ipAddress?: string | null;
  action: string | null;
  previousDefaultLanguage: string | null;
  newDefaultLanguage: string | null;
  inviteTemplateEnChanged?: boolean | null;
  inviteTemplateFrChanged?: boolean | null;
}

export function useSystemLogs(limit = 3) {
  return useQuery<OrgSettingsLogEntry[]>({
    queryKey: ['system-logs', limit],
    queryFn: async () => {
      const res = await axiosInstance.get<OrgSettingsLogEntry[]>(`/api/admin/settings/audit?limit=${limit}`);
      return res.data;
    },
    refetchInterval: 10000, // Poll every 10 seconds for live updates
  });
}
