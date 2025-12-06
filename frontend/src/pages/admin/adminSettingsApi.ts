import axiosInstance from "@/api/axiosInstance";

// Type of the organization settings payload
export interface UpdateOrganizationSettingsRequest {
  defaultLanguage: string;
  inviteSubjectEn: string;
  inviteBodyEn: string;
  inviteSubjectFr: string;
  inviteBodyFr: string;
}

// GET /api/admin/settings
export async function getOrganizationSettings() {
  const res = await axiosInstance.get("/api/admin/settings");
  return res.data;
}

// PUT /api/admin/settings
export async function updateOrganizationSettings(
  payload: UpdateOrganizationSettingsRequest
) {
  const res = await axiosInstance.put("/api/admin/settings", payload);
  return res.data;
}
