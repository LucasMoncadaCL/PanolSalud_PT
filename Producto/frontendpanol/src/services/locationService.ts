import { apiClient } from "./apiClient";
import type { LocationOption } from "../types/location";

export async function fetchLocations(): Promise<LocationOption[]> {
  const response = await apiClient.get<LocationOption[]>("/api/v2/locations");
  return response.data;
}

export async function fetchLocationsForManagement(): Promise<LocationOption[]> {
  const response = await apiClient.get<LocationOption[]>("/api/v2/locations/management");
  return response.data;
}

export async function createLocation(payload: { name: string; description: string | null }): Promise<LocationOption> {
  const response = await apiClient.post<LocationOption>("/api/v2/locations", payload);
  return response.data;
}

export async function updateLocation(
  locationUuid: string,
  payload: { name: string; description: string | null },
): Promise<LocationOption> {
  const response = await apiClient.put<LocationOption>(`/api/v2/locations/${locationUuid}`, payload);
  return response.data;
}

export async function setLocationActive(locationUuid: string, active: boolean): Promise<LocationOption> {
  const response = await apiClient.patch<LocationOption>(`/api/v2/locations/${locationUuid}/active`, null, { params: { active } });
  return response.data;
}

