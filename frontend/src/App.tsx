import { useEffect } from "react";
import { useAuth0 } from "@auth0/auth0-react";
import { AppRoutes } from "@/app/routes/AppRoutes";
import { getRoleFromUser, type AppRole } from "@/features/auth/roleUtils";

// Check if we're in the middle of an Auth0 callback (URL has code and state params)
function isAuth0Callback(): boolean {
  const params = new URLSearchParams(window.location.search);
  return params.has("code") && params.has("state");
}

export default function App() {
  const { isAuthenticated, isLoading, user, loginWithRedirect } = useAuth0();

  const role: AppRole | null = getRoleFromUser(user);

  // Don't redirect if we're processing an Auth0 callback
  const isProcessingCallback = isAuth0Callback();

  useEffect(() => {
    // Skip redirect if still loading, processing callback, or already authenticated with a role
    if (isLoading || isProcessingCallback) {
      return;
    }

    if (!isAuthenticated || !role) {
      loginWithRedirect({
        appState: { returnTo: window.location.pathname },
      });
    }
  }, [isLoading, isAuthenticated, role, loginWithRedirect, isProcessingCallback]);

  if (isLoading || isProcessingCallback || !isAuthenticated || !role) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <p>{isLoading || isProcessingCallback ? "Loading..." : "Redirecting to login..."}</p>
      </div>
    );
  }

  return <AppRoutes />;
}

