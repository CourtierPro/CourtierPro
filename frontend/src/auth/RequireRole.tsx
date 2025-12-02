import type { ReactNode } from "react";
import { Navigate } from "react-router-dom";
import { useAuth0 } from "@auth0/auth0-react";
import { getRoleFromUser, type AppRole } from "./roleUtils";

interface RequireRoleProps {
    allowed?: AppRole[];
    children: ReactNode;
}


export function RequireRole({ allowed, children }: RequireRoleProps) {
    const { user } = useAuth0();

    const role = getRoleFromUser(user);

    if (!role) {
        return <Navigate to="/unauthorized" replace />;
    }

    if (allowed && !allowed.includes(role)) {
        return <Navigate to="/forbidden" replace />;
    }

    return <>{children}</>;
}
