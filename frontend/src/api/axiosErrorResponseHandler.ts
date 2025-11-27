import type { AxiosError } from "axios";

// Map status codes to their respective error pages
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
  if (statusCode === 401) {
    // TODO: integrate with Auth0 logout + optional local cleanup
    console.error("Session expired. Redirecting to unauthorized page.");

    // Example: flag to show a session-expired message on login
    sessionStorage.setItem("sessionExpired", "true");
  }

  const redirectPath = errorPageRedirects[statusCode];

  if (redirectPath && typeof window !== "undefined") {
    console.error(`Redirecting to ${redirectPath} due to server error:`, error);
    window.location.href = redirectPath;
  } else {
    console.error("Unhandled global error:", error, "Status code:", statusCode);
  }
}
