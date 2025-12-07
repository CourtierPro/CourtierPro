import { type ReactNode, useEffect } from "react";
import { useAuth0 } from "@auth0/auth0-react";
import { useTranslation } from "react-i18next";

interface RequireAuthProps {
  children: ReactNode;
}


export function RequireAuth({ children }: RequireAuthProps) {
  const { t } = useTranslation("common");
  const { isAuthenticated, isLoading, loginWithRedirect } = useAuth0();

  useEffect(() => {
    if (!isLoading && !isAuthenticated) {
      void loginWithRedirect({
        appState: { returnTo: window.location.pathname },
      });
    }
  }, [isLoading, isAuthenticated, loginWithRedirect]);

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
