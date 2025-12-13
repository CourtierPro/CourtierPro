import axiosInstance from "@/shared/api/axiosInstance";

export type FeedbackType = "bug" | "feature";

export interface FeedbackRequest {
  type: FeedbackType;
  message: string;
}

export interface FeedbackResponse {
  success: boolean;
  issueUrl?: string;
  issueNumber?: number;
}

/**
 * Submit user feedback to create a GitHub issue
 */
export async function submitFeedback(feedback: FeedbackRequest): Promise<FeedbackResponse> {
  const response = await axiosInstance.post<FeedbackResponse>("/api/v1/feedback", feedback);
  return response.data;
}
