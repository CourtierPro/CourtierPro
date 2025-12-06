import type { ReactNode } from "react";
import { Navigate } from "react-router-dom";
import { useAuth0 } from "@auth0/auth0-react";
import { getRoleFromUser, type AppRole } from "@/features/auth/roleUtils";

interface RequireRoleProps {
    allowed?: AppRole[];
    children: ReactNode;
}


export function RequireRole({ allowed, children }: RequireRoleProps) {
    const { user, isLoading } = useAuth0();

    if (isLoading) {
        return (
            <div className="flex min-h-screen items-center justify-center">
                <div className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
            </div>
        );
    }

    const role = getRoleFromUser(user);

    if (!role) {
        return <Navigate to="/unauthorized" replace />;
    }

    if (allowed && !allowed.includes(role)) {
        return <Navigate to="/forbidden" replace />;
    }

    return <>{children}</>;
}
