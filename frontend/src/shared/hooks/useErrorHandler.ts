import { useCallback, useState } from "react";
import { AppError, getErrorMessage, logError } from "@/shared/utils/error-utils";

export interface ErrorState {
  error: Error | null;
  message: string | null;
}

/**
 * useErrorHandler centralizes common error handling concerns:
 * - Normalizing unknown error values into Error + message
 * - Logging via logError
 * - Exposing convenient state & helpers for UI
 */
export function useErrorHandler(initialError: Error | string | null = null) {
  const [errorState, setErrorState] = useState<ErrorState>(() => {
    if (!initialError) {
      return { error: null, message: null };
    }

    const error =
      initialError instanceof Error ? initialError : new AppError(String(initialError));

    return {
      error,
      message: getErrorMessage(error),
    };
  });

  const setError = useCallback((error: Error | string | null) => {
    if (!error) {
      setErrorState({ error: null, message: null });
      return;
    }

    const normalizedError = error instanceof Error ? error : new AppError(String(error));
    const message = getErrorMessage(normalizedError);

    setErrorState({ error: normalizedError, message });
  }, []);

  const handleError = useCallback((error: unknown) => {
    // Normalize to Error instance
    const message = getErrorMessage(error);
    const normalizedError =
      error instanceof Error ? error : new AppError(message, { cause: error });

    // Log once, centrally
    logError(normalizedError);

    setErrorState({ error: normalizedError, message });
  }, []);

  const clearError = useCallback(() => {
    setErrorState({ error: null, message: null });
  }, []);

  const hasError = !!errorState.error;

  return {
    error: errorState.error,
    message: errorState.message,
    hasError,
    setError,
    handleError,
    clearError,
  };
}
