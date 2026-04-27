import { AxiosError } from "axios";
import { apiClient } from "./apiClient";
import type { ActiveCategoryOption } from "../types/categoryActive";

async function fetchFrom(path: string): Promise<ActiveCategoryOption[]> {
  const response = await apiClient.get<ActiveCategoryOption[]>(path);
  return response.data;
}

export async function fetchActiveCategories(): Promise<ActiveCategoryOption[]> {
  try {
    return await fetchFrom("/api/categories/active");
  } catch (error) {
    if (error instanceof AxiosError && error.response?.status === 404) {
      return await fetchFrom("/api/categorias/active");
    }
    throw error;
  }
}

