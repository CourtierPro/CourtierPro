import { type ReactNode, useEffect } from "react";
import { useAuth0 } from "@auth0/auth0-react";
import { useTranslation } from "react-i18next";

interface RequireAuthProps {
  children: ReactNode;
}


export function RequireAuth({ children }: RequireAuthProps) {
  const { t } = useTranslation("common");

  const authDisabled = import.meta.env.VITE_AUTH_DISABLED === "true";

  const { isAuthenticated: rawIsAuthenticated, isLoading: rawIsLoading, loginWithRedirect } = useAuth0();

  const isLoading = authDisabled ? false : rawIsLoading;
  const isAuthenticated = authDisabled ? true : rawIsAuthenticated;

  useEffect(() => {
    if (authDisabled) return;

    if (!isLoading && !isAuthenticated) {
      void loginWithRedirect({
        appState: { returnTo: window.location.pathname },
      });
    }
  }, [authDisabled, isLoading, isAuthenticated, loginWithRedirect]);

  if (authDisabled) return <>{children}</>;

  if (isLoading) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <p>{t("status.loading")}</p>
      </div>
    );
  }

  if (!isAuthenticated) {
    return null;
  }

  return <>{children}</>;
}
