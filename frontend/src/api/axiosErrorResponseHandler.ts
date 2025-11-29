import type { AxiosError } from "axios";

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
  // 🔒 Auth0-specific handling (future step)
  if (statusCode === 401) {
    console.warn("401 Unauthorized: likely missing/expired Auth0 token");

    // FE will use this to show “session expired” toast on login
    sessionStorage.setItem("sessionExpired", "true");

    // OPTIONAL (later): redirect to Auth0 login
    // window.location.href = "/login";
  }

  // Handle known global errors
  const redirectPath = errorPageRedirects[statusCode];

  if (redirectPath && typeof window !== "undefined") {
    console.error(`[${statusCode}] redirecting to: ${redirectPath}`, error);
    window.location.href = redirectPath;
    return;
  }

  // Fallback for unknown/unmapped errors
  console.error("Unhandled global error:", error, "Status:", statusCode);
}
