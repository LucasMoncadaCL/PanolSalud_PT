import { apiClient } from "./apiClient";
import type {
  ImplementCreatePayload,
  ImplementDetail,
  ImplementFilters,
  ImplementSummary,
  ImplementUpdatePayload,
} from "../types/implement";

export async function fetchImplements(filters?: ImplementFilters): Promise<ImplementSummary[]> {
  const response = await apiClient.get<ImplementSummary[]>("/api/implements", {
    params: {
      name: filters?.name || undefined,
      categoryId: filters?.categoryId ?? undefined,
    },
  });
  return response.data;
}

export async function createImplement(payload: ImplementCreatePayload): Promise<ImplementDetail> {
  const response = await apiClient.post<ImplementDetail>("/api/implements", payload);
  return response.data;
}

export async function updateImplement(
  implementId: number,
  payload: ImplementUpdatePayload,
): Promise<void> {
  await apiClient.put(`/api/implements/${implementId}`, payload);
}

export async function fetchImplementById(implementId: number): Promise<ImplementDetail> {
  const response = await apiClient.get<ImplementDetail>(`/api/implements/${implementId}`);
  return response.data;
}
