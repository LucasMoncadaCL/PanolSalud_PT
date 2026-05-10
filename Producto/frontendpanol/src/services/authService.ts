import { apiClient } from "./apiClient";
import { clearSession, setAccessToken } from "../utils/auth";

export interface LoginPayload {
  rut: string;
  password: string;
  rememberMe?: boolean;
}

export interface LoginResult {
  accessToken: string;
  role: string;
  expiresInSeconds: number;
}

export async function login(payload: LoginPayload): Promise<LoginResult> {
  const { rememberMe = true, ...requestPayload } = payload;
  const { data } = await apiClient.post<LoginResult>("/api/v2/auth/login", requestPayload);
  setAccessToken(data.accessToken, rememberMe);
  return data;
}

export async function logout(): Promise<void> {
  try {
    await apiClient.post("/api/v2/auth/logout");
  } finally {
    clearSession();
  }
}
