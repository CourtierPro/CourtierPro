import { useCallback } from 'react';
import { useAuth0 } from '@auth0/auth0-react';
import axiosInstance from '@/shared/api/axiosInstance';

export interface LogoutOptions {
  /**
   * Reason for logout (e.g., 'manual', 'session_timeout', 'forced')
   */
  reason?: 'manual' | 'session_timeout' | 'forced';
}

/**
 * Hook that provides a centralized logout function
 * Handles:
 * - Logging logout event to backend
 * - Clearing Auth0 session
 * - Clearing local storage/session storage
 * - Redirecting to login page
 * 
 * @example
 * ```tsx
 * const handleLogout = useLogout();
 * 
 * // Manual logout
 * <button onClick={() => handleLogout({ reason: 'manual' })}>
 *   Logout
 * </button>
 * 
 * // Session timeout
 * useSessionTimeout({
 *   onTimeout: () => handleLogout({ reason: 'session_timeout' })
 * });
 * ```
 */
export function useLogout() {
  const { logout: auth0Logout } = useAuth0();

  const handleLogout = useCallback(
    async (options: LogoutOptions = {}) => {
      const { reason = 'manual' } = options;

      try {
        // 1. Log the logout event to backend
        await axiosInstance.post('/auth/logout', {
          reason,
          timestamp: new Date().toISOString(),
        }).catch((error) => {
          // Don't block logout if logging fails
          console.error('Failed to log logout event:', error);
        });

        // 2. Clear any local storage items (tokens, user data, etc.)
        // Auth0 SDK handles its own token storage, but clear any app-specific items
        sessionStorage.removeItem('sessionExpired');
        // Add any other items you want to clear:
        // localStorage.removeItem('yourAppData');

        // 3. Call Auth0 logout endpoint
        // This will:
        // - Clear Auth0 session
        // - Clear Auth0 tokens from storage
        // - Redirect to Auth0 logout endpoint
        // - Then redirect back to returnTo URL
        auth0Logout({
          logoutParams: {
            returnTo: window.location.origin,
          },
        });
      } catch (error) {
        console.error('Logout error:', error);
        
        // Even if there's an error, still attempt Auth0 logout
        auth0Logout({
          logoutParams: {
            returnTo: window.location.origin,
          },
        });
      }
    },
    [auth0Logout]
  );

  return handleLogout;
}
