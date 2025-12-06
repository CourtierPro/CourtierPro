// src/auth/roleUtils.ts
import type { User } from "@auth0/auth0-react";

export type AppRole = "broker" | "client" | "admin";


export function getRoleFromUser(user: User | undefined | null): AppRole | null
{
    const rolesClaim = "https://courtierpro.dev/roles";

    const roles = (user?.[rolesClaim] as string[] | undefined) ?? [];

    if (roles.includes("ADMIN")) return "admin";
    if (roles.includes("BROKER")) return "broker";
    if (roles.includes("CLIENT")) return "client";

    return null;
}


export function getPreferredLanguage(
    user: User | undefined | null
): "en" | "fr" {
    const langClaim = "https://courtierpro.dev/lang";
    const lang = (user?.[langClaim] as string | undefined) ?? "en";

    return lang === "fr" ? "fr" : "en";
}
