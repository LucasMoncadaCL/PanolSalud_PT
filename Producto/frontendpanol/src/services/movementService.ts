import { apiClient } from "./apiClient";
import type { InventoryMovementDetail } from "../types/implement";

export type ManualMovementType = "INGRESO" | "AJUSTE";

export interface RegisterMovementPayload {
  action: ManualMovementType;
  quantity: number;
  notes?: string | null;
}

export async function registerManualMovement(
  implementUuid: string,
  payload: RegisterMovementPayload,
): Promise<InventoryMovementDetail> {
  const response = await apiClient.post<InventoryMovementDetail>(
    `/api/v2/implements/${implementUuid}/movements`,
    payload,
  );
  return response.data;
}

export async function fetchInventoryMovements(): Promise<InventoryMovementDetail[]> {
  const response = await apiClient.get<InventoryMovementDetail[]>("/api/v2/implements/movements");
  return response.data;
}
