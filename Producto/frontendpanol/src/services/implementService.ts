import { apiClient } from "./apiClient";
import type { ImplementCreatePayload } from "../types/implement";

export async function createImplement(payload: ImplementCreatePayload): Promise<void> {
  await apiClient.post("/api/implements", payload);
}

