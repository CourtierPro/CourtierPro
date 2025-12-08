import { useEffect, useRef, useCallback } from 'react';

interface UseSessionTimeoutOptions {
  /**
   * Timeout duration in milliseconds
   * Default: 30 minutes (1800000ms)
   */
  timeout?: number;
  
  /**
   * Callback function to execute when session times out
   */
  onTimeout: () => void;
  
  /**
   * Whether the session timeout is enabled
   * Default: true
   */
  enabled?: boolean;
}

/**
 * Hook that tracks user activity and triggers a callback after a period of inactivity
 * 
 * @example
 * ```tsx
 * useSessionTimeout({
 *   timeout: 30 * 60 * 1000, // 30 minutes
 *   onTimeout: handleLogout,
 *   enabled: isAuthenticated
 * });
 * ```
 */
export function useSessionTimeout({
  timeout = 30 * 60 * 1000, // 30 minutes default
  onTimeout,
  enabled = true,
}: UseSessionTimeoutOptions) {
  const timeoutIdRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const onTimeoutRef = useRef(onTimeout);

  // Keep the callback ref up to date
  useEffect(() => {
    onTimeoutRef.current = onTimeout;
  }, [onTimeout]);

  const resetTimer = useCallback(() => {
    // Clear existing timeout
    if (timeoutIdRef.current) {
      clearTimeout(timeoutIdRef.current);
    }

    // Set new timeout
    timeoutIdRef.current = setTimeout(() => {
      onTimeoutRef.current();
    }, timeout);
  }, [timeout]);

  useEffect(() => {
    if (!enabled) {
      // Clear timeout if disabled
      if (timeoutIdRef.current) {
        clearTimeout(timeoutIdRef.current);
      }
      return;
    }

    // Events that indicate user activity
    const events = [
      'mousedown',
      'mousemove',
      'keydown',
      'scroll',
      'touchstart',
      'click',
    ];

    // Reset timer on any user activity
    const handleActivity = () => {
      resetTimer();
    };

    // Add event listeners
    events.forEach((event) => {
      document.addEventListener(event, handleActivity);
    });

    // Initialize the timer
    resetTimer();

    // Cleanup
    return () => {
      events.forEach((event) => {
        document.removeEventListener(event, handleActivity);
      });
      
      if (timeoutIdRef.current) {
        clearTimeout(timeoutIdRef.current);
      }
    };
  }, [enabled, resetTimer]);
}
