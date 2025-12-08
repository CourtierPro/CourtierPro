import { useEffect } from "react";
import { useAuth0 } from "@auth0/auth0-react";
import { AppRoutes } from "@/app/routes/AppRoutes";
import { getRoleFromUser, type AppRole } from "@/features/auth/roleUtils";

export default function App() {
  // When running Playwright locally disable auth and render routes directly
  if (import.meta.env.VITE_AUTH_DISABLED === "true") {
    return <AppRoutes />;
  }

  const { isAuthenticated, isLoading, user, loginWithRedirect } = useAuth0();

  const role: AppRole | null = getRoleFromUser(user);

  useEffect(() => {
    if (!isLoading && (!isAuthenticated || !role)) {
      loginWithRedirect({
        appState: { returnTo: window.location.pathname },
      });
    }
  }, [isLoading, isAuthenticated, role, loginWithRedirect]);

  if (isLoading || !isAuthenticated || !role) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <p>{isLoading ? "Loading..." : "Redirecting to login..."}</p>
      </div>
    );
  }

  return <AppRoutes />;
}
