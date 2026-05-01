export type UserRole = "COORDINADOR" | "DIRECTOR" | "DOCENTE" | "UNKNOWN";

export function getUserRoleFromToken(): UserRole {
  try {
    const rawToken = localStorage.getItem("access_token");
    if (!rawToken) {
      return "UNKNOWN";
    }

    const payloadPart = rawToken.split(".")[1];
    if (!payloadPart) {
      return "UNKNOWN";
    }

    const normalized = payloadPart.replace(/-/g, "+").replace(/_/g, "/");
    const jsonPayload = decodeURIComponent(
      atob(normalized)
        .split("")
        .map((char) => `%${(`00${char.charCodeAt(0).toString(16)}`).slice(-2)}`)
        .join("")
    );

    const parsed = JSON.parse(jsonPayload) as {
      role?: string;
      user_role?: string;
      roles?: string[];
      app_metadata?: { role?: string; roles?: string[] };
    };

    const roles = [
      parsed.role,
      parsed.user_role,
      ...(parsed.roles ?? []),
      parsed.app_metadata?.role,
      ...(parsed.app_metadata?.roles ?? []),
    ]
      .filter(Boolean)
      .map((role) => String(role).replace("ROLE_", "").toUpperCase());

    if (roles.includes("COORDINADOR")) return "COORDINADOR";
    if (roles.includes("DIRECTOR")) return "DIRECTOR";
    if (roles.includes("DOCENTE")) return "DOCENTE";
    
    return "UNKNOWN";
  } catch {
    return "UNKNOWN";
  }
}
