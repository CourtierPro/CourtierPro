import axiosInstance from "@/shared/api/axiosInstance";


// Type of the organization settings payload
export interface UpdateOrganizationSettingsRequest {
  defaultLanguage: string;
  // Invite Template
  inviteSubjectEn: string;
  inviteBodyEn: string;
  inviteSubjectFr: string;
  inviteBodyFr: string;
  // Document Submitted Template
  documentSubmittedSubjectEn?: string;
  documentSubmittedBodyEn?: string;
  documentSubmittedSubjectFr?: string;
  documentSubmittedBodyFr?: string;
  // Document Requested Template
  documentRequestedSubjectEn?: string;
  documentRequestedBodyEn?: string;
  documentRequestedSubjectFr?: string;
  documentRequestedBodyFr?: string;
  // Document Review Template
  documentReviewSubjectEn?: string;
  documentReviewBodyEn?: string;
  documentReviewSubjectFr?: string;
  documentReviewBodyFr?: string;
  // Stage Update Template
  stageUpdateSubjectEn?: string;
  stageUpdateBodyEn?: string;
  stageUpdateSubjectFr?: string;
  stageUpdateBodyFr?: string;
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
