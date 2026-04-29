import { apiClient } from "./apiClient";
import type {
  Categoria,
  CategoriaAssociationSummary,
  CategoriaPayload,
} from "../types/category";

const basePath = "/api/categorias";

export async function fetchCategoriasGestion(): Promise<Categoria[]> {
  const response = await apiClient.get<Categoria[]>(`${basePath}/gestion`);
  return response.data;
}

export async function fetchCategoriaAssociation(
  categoryId: number,
): Promise<CategoriaAssociationSummary> {
  const response = await apiClient.get<{
    categoriaId?: number;
    categoryId?: number;
    implementosAsociados?: number;
    implementCount?: number;
    canDelete: boolean;
  }>(
    `${basePath}/${categoryId}/asociaciones`,
  );

  return {
    categoryId: response.data.categoryId ?? response.data.categoriaId ?? categoryId,
    implementCount: response.data.implementCount ?? response.data.implementosAsociados ?? 0,
    canDelete: response.data.canDelete,
  };
}

export async function createCategoria(payload: CategoriaPayload): Promise<Categoria> {
  const response = await apiClient.post<Categoria>(basePath, payload);
  return response.data;
}

export async function updateCategoria(
  categoryId: number,
  payload: CategoriaPayload,
): Promise<Categoria> {
  const response = await apiClient.put<Categoria>(`${basePath}/${categoryId}`, payload);
  return response.data;
}

export async function deactivateCategoria(
  categoryId: number,
  force: boolean,
): Promise<Categoria> {
  const response = await apiClient.patch<Categoria>(
    `${basePath}/${categoryId}/desactivar?force=${force}`,
  );
  return response.data;
}

export async function deleteCategoria(categoryId: number): Promise<void> {
  await apiClient.delete(`${basePath}/${categoryId}`);
}

