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

  // AppError or regular Error
  if (error instanceof AppError || error instanceof Error) {
    return error.message || fallback;
  }

  // AxiosError with response payload
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
