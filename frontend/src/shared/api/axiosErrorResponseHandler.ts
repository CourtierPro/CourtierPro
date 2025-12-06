import type { AxiosError } from "axios";
import {
  AppError,
  UnauthorizedError,
  ForbiddenError,
  NotFoundError,
  logError,
} from "@/shared/utils/error-utils";

// Centralized redirect mapping
const errorPageRedirects: Record<number, string> = {
  401: "/unauthorized",
  403: "/forbidden",
  500: "/internal-server-error",
  503: "/service-unavailable",
};

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
    "An unexpected error occurred";

  // Create appropriate AppError for consistent logging
  let appError: AppError;
  switch (statusCode) {
    case 401:
      appError = new UnauthorizedError(message, { statusCode, cause: error });
      // FE will use this to show "session expired" toast on login
      sessionStorage.setItem("sessionExpired", "true");
      break;
    case 403:
      appError = new ForbiddenError(message, { statusCode, cause: error });
      break;
    case 404:
      appError = new NotFoundError(message, { statusCode, cause: error });
      break;
    default:
      appError = new AppError(message, { statusCode, cause: error });
  }

  // Centralized logging
  logError(appError);

  // Handle known global errors with redirects
  const redirectPath = errorPageRedirects[statusCode];

  if (redirectPath && typeof window !== "undefined") {
    window.location.href = redirectPath;
    return;
  }
}
