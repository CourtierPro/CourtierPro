import type { AxiosError } from "axios";
import {
  AppError,
  UnauthorizedError,
  ForbiddenError,
  logError,
} from "@/shared/utils/error-utils";
import { toast } from "sonner";
import i18next from "i18next";

export default function axiosErrorResponseHandler(
  error: AxiosError,
  statusCode: number
): void {
  // Extract message from response or error
  const responseData = error.response?.data as { message?: string; error?: string } | undefined;
  const message =
    responseData?.message ||
    responseData?.error ||
    error.message ||
    i18next.t("common:error.unexpectedError");

  // Create appropriate AppError for consistent logging
  let appError: AppError;
  switch (statusCode) {
    case 401:
      appError = new UnauthorizedError(message, { statusCode, cause: error });
      // Mark session as expired for login page to show toast
      sessionStorage.setItem("sessionExpired", "true");
      // Show toast instead of hard redirect
      toast.error(i18next.t("common:error.sessionExpired"));
      break;
    case 403:
      appError = new ForbiddenError(message, { statusCode, cause: error });
      // Show toast for permission error - don't redirect authenticated users
      toast.error(i18next.t("common:error.noPermission"));
      break;
    case 500:
      appError = new AppError(message, { statusCode, cause: error });
      toast.error(i18next.t("common:error.serverError"));
      break;
    case 503:
      appError = new AppError(message, { statusCode, cause: error });
      toast.error(i18next.t("common:error.serviceUnavailable"));
      break;
    default:
      appError = new AppError(message, { statusCode, cause: error });
  }

  // Centralized logging
  logError(appError);

  // Note: We no longer do hard redirects here.
  // Components should handle errors via TanStack Query's error states
  // or the thrown error from axios.
}
