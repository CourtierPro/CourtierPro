import { useEffect } from "react";
import { useAuth0 } from "@auth0/auth0-react";
import { AppRoutes } from "@/app/routes/AppRoutes";
import { getRoleFromUser, type AppRole } from "@/features/auth/roleUtils";

export default function App() {
  const authDisabled = import.meta.env.VITE_AUTH_DISABLED === "true";

  const { isAuthenticated: rawIsAuthenticated, isLoading: rawIsLoading, user, loginWithRedirect } = useAuth0();

  // When auth is disabled for Playwright, override values so app treats user as authenticated
  const isLoading = authDisabled ? false : rawIsLoading;
  const isAuthenticated = authDisabled ? true : rawIsAuthenticated;

  const role: AppRole | null = authDisabled ? "broker" : getRoleFromUser(user);

  useEffect(() => {
    if (authDisabled) return;

    if (!isLoading && (!isAuthenticated || !role)) {
      void loginWithRedirect({
        appState: { returnTo: window.location.pathname },
      });
    }
  }, [authDisabled, isLoading, isAuthenticated, role, loginWithRedirect]);

  if (isLoading || !isAuthenticated || !role) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <p>{isLoading ? "Loading..." : "Redirecting to login..."}</p>
      </div>
    );
  }

  return <AppRoutes />;
}
