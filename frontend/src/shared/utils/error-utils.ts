import type { AxiosError } from "axios";

/**
 * Base application error class.
 * Extend this for domain-specific errors so we can
 * reliably distinguish them from generic Error objects.
 */
export class AppError extends Error {
  public readonly statusCode?: number;
  public readonly cause?: unknown;

  constructor(message: string, options?: { statusCode?: number; cause?: unknown }) {
    super(message);

    this.name = new.target.name;

    if (options?.statusCode !== undefined) {
      this.statusCode = options.statusCode;
    }

    if (options?.cause !== undefined) {
      this.cause = options.cause;
    }

    Object.setPrototypeOf(this, new.target.prototype);
  }
}

/**
 * Common HTTP / domain-specific errors.
 * You can add more as needed (e.g. ConflictError, RateLimitError, etc.).
 */
export class UnauthorizedError extends AppError { }
export class ForbiddenError extends AppError { }
export class NotFoundError extends AppError { }
export class ValidationError extends AppError { }

/**
 * Type guard to detect Axios errors safely.
 */
export function isAxiosError(error: unknown): error is AxiosError {
  return typeof error === "object" && error !== null && "isAxiosError" in error;
}

/**
 * Safely extract a human-readable message from any unknown error value.
 * This is what you should show in toasts, banners, dialogs, etc.
 */
export function getErrorMessage(error: unknown, fallback = "An unexpected error occurred."): string {
  if (!error) {
    return fallback;
  }

  // Check for axios error with response payload FIRST (before generic Error check)
  // This is important because AxiosError extends Error, and we want response.data.error/message
  if (isAxiosError(error)) {
    const axiosError = error as AxiosError<Record<string, unknown>>;

    const data = axiosError.response?.data;

    if (data) {
      if (typeof data.message === "string" && data.message.trim().length > 0) {
        return data.message;
      }

      if (typeof data.error === "string" && data.error.trim().length > 0) {
        return data.error;
      }
    }

    if (axiosError.response?.statusText) {
      return axiosError.response.statusText;
    }

    // Only fall back to axios error.message if no response data
    if (axiosError.message) {
      return axiosError.message;
    }
  }

  // Handle error objects that might have response inside (e.g., from mutations)
  if (typeof error === "object") {
    const err = error as Record<string, unknown>;

    // Check for nested response structure
    if (err.response && typeof err.response === "object") {
      const resp = err.response as Record<string, unknown>;
      if (resp.data && typeof resp.data === "object") {
        const data = resp.data as Record<string, unknown>;
        if (typeof data.message === "string" && data.message.trim().length > 0) {
          return data.message;
        }
        if (typeof data.error === "string" && data.error.trim().length > 0) {
          return data.error;
        }
      }
    }
  }

  // AppError messages (custom app errors, not axios)
  if (error instanceof AppError) {
    return error.message || fallback;
  }

  // Generic Error - but skip if it looks like a default axios message
  if (error instanceof Error) {
    const msg = error.message;
    // Don't return generic axios messages like "Request failed with status code 400"
    if (msg && !msg.includes("Request failed with status code")) {
      return msg;
    }
  }

  if (typeof error === "string") {
    return error;
  }

  return fallback;
}

/**
 * Central logging hook for errors.
 * Right now this just logs to the console, but you can later
 * plug in a service like Sentry, Datadog, etc. here.
 */
export function logError(error: Error): void {
  console.error("[AppError]", {
    name: error.name,
    message: error.message,
    stack: error.stack,
  });
}
