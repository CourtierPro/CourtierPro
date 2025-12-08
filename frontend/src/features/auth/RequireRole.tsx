import type { ReactNode } from "react";
import { Navigate } from "react-router-dom";
import { useAuth0 } from "@auth0/auth0-react";
import { getRoleFromUser, type AppRole } from "@/features/auth/roleUtils";

interface RequireRoleProps {
    allowed?: AppRole[];
    children: ReactNode;
}


export function RequireRole({ allowed, children }: RequireRoleProps) {
    const authDisabled = import.meta.env.VITE_AUTH_DISABLED === "true";

    const { user, isLoading: rawIsLoading } = useAuth0();
    const isLoading = authDisabled ? false : rawIsLoading;

    if (isLoading) {
        return (
            <div className="flex min-h-screen items-center justify-center">
                <div className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
            </div>
        );
    }

    const role = authDisabled ? ("broker" as AppRole) : getRoleFromUser(user);

    if (!role) {
        return <Navigate to="/unauthorized" replace />;
    }

    if (allowed && !allowed.includes(role)) {
        return <Navigate to="/forbidden" replace />;
    }

    return <>{children}</>;
}
