// frontend/src/pages/admin/systemLogsApi.ts
import axiosInstance from "@/shared/api/axiosInstance";

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

export async function fetchOrgSettingsLogs(): Promise<OrgSettingsLogEntry[]> {
  const response = await axiosInstance.get<OrgSettingsLogEntry[]>(
    "/api/admin/settings/audit"
  );
  return response.data;
}
