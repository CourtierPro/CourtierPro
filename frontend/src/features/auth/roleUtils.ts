import type { User } from "@auth0/auth0-react";

export type AppRole = "broker" | "client" | "admin";


export function getRoleFromUser(user: User | undefined | null): AppRole | null {
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


/**
 * Gets the test role from the VITE_TEST_ROLE environment variable.
 * Used when authentication is disabled to determine which role to simulate.
 * 
 * @returns The configured test role, or "broker" if not set or invalid
 * 
 * Supported values:
 * - "broker": Broker role (default)
 * - "client": Client role
 * - "admin": Admin role
 * 
 * Any other value will default to "broker" for backward compatibility.
 */
export function getTestRole(): AppRole {
    const testRole = import.meta.env.VITE_TEST_ROLE as string | undefined;
    
    if (testRole === "client") return "client";
    if (testRole === "admin") return "admin";
    if (testRole === "broker") return "broker";
    
    // Warn if an invalid value was provided
    if (testRole && testRole !== "broker" && testRole !== "client" && testRole !== "admin") {
        console.warn(`Invalid VITE_TEST_ROLE value: "${testRole}". Defaulting to "broker". Valid values are: "broker", "client", "admin".`);
    }
    
    // Default to "broker" for backward compatibility
    // This includes cases where VITE_TEST_ROLE is undefined or has an invalid value
    return "broker";
}
