import { useCallback, useEffect, useState } from "react";
import { Plus } from "lucide-react";
import { InventoryLayout } from "../components/layout/InventoryLayout";
import { ImplementFormModal } from "../components/implements/ImplementFormModal";
import { ImplementEditModal } from "../components/implements/ImplementEditModal";
import { createImplement, fetchImplements, updateImplement } from "../services/implementService";
import { fetchActiveCategories } from "../services/activeCategoryService";
import { getErrorMessage } from "../services/apiClient";
import type { ActiveCategoryOption } from "../types/categoryActive";
import type { ImplementFilters, ImplementStockFilterStatus, ImplementSummary } from "../types/implement";

type UserRole = "COORDINADOR" | "DIRECTOR" | "DOCENTE" | "UNKNOWN";

export function InventoryItemsPage() {
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [editing, setEditing] = useState<ImplementSummary | null>(null);
  const [saving, setSaving] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [implementos, setImplementos] = useState<ImplementSummary[]>([]);
  const [totalImplements, setTotalImplements] = useState(0);
  const [categoryOptions, setCategoryOptions] = useState<ActiveCategoryOption[]>([]);
  const [categoriesLoading, setCategoriesLoading] = useState(false);
  const [userRole, setUserRole] = useState<UserRole>("UNKNOWN");
  const [filters, setFilters] = useState<ImplementFilters>({
    name: "",
    categoryId: null,
    stockStatus: "all",
  });
  const [debouncedNameFilter, setDebouncedNameFilter] = useState(filters.name ?? "");

  const refreshImplements = useCallback(async () => {
    setLoading(true);
    setError(null);

    try {
      const [rows, allRows] = await Promise.all([
        fetchImplements({
          name: debouncedNameFilter.trim() || undefined,
          categoryId: filters.categoryId ?? null,
        }),
        fetchImplements(),
      ]);

      const stockStatus = filters.stockStatus ?? "all";
      const filteredRows = rows.filter((row) => {
        if (stockStatus === "all") {
          return true;
        }

        const available = row.stock?.available ?? 0;
        const minStock = row.stock?.min_stock ?? 0;

        if (stockStatus === "with_stock") {
          return available > 0;
        }
        if (stockStatus === "without_stock") {
          return available <= 0;
        }
        if (stockStatus === "low_stock") {
          return available <= minStock;
        }
        return true;
      });

      setImplementos(filteredRows);
      setTotalImplements(allRows.length);
    } catch (error) {
      setError(getErrorMessage(error, "No se pudo cargar el listado de implementos."));
    } finally {
      setLoading(false);
    }
  }, [debouncedNameFilter, filters.categoryId, filters.stockStatus]);

  const hasActiveFilters =
    (filters.name ?? "").trim().length > 0 ||
    filters.categoryId != null ||
    (filters.stockStatus ?? "all") !== "all";

  function clearFilters() {
    setFilters({
      name: "",
      categoryId: null,
      stockStatus: "all",
    });
  }

  useEffect(() => {
    refreshImplements();
  }, [refreshImplements]);

  useEffect(() => {
    const timeout = window.setTimeout(() => setDebouncedNameFilter(filters.name ?? ""), 350);
    return () => window.clearTimeout(timeout);
  }, [filters.name]);

  useEffect(() => {
    async function loadActiveCategories() {
      setCategoriesLoading(true);
      try {
        const categories = await fetchActiveCategories();
        setCategoryOptions(categories);
      } catch {
        setCategoryOptions([]);
      } finally {
        setCategoriesLoading(false);
      }
    }

    loadActiveCategories();
  }, []);

  useEffect(() => {
    setUserRole(getUserRoleFromToken());
  }, []);

  const isCoordinator = userRole === "COORDINADOR";

  async function handleSubmit(payload: {
    name: string;
    categoryId: number;
    itemType: "consumable" | "reusable" | "individual";
    locationId: number;
    description: string | null;
    minStock: number;
    observations: string | null;
  }) {
    setSaving(true);
    setError(null);
    setSuccess(null);

    try {
      const created = await createImplement({
        name: payload.name,
        category_id: payload.categoryId,
        item_type: payload.itemType,
        location_id: payload.locationId,
        description: payload.description,
        min_stock: payload.minStock,
        observations: payload.observations,
      });
      try {
        window.sessionStorage.setItem("inventory.justCreatedImplementId", String(created.id));
      } catch {
        // Si el storage no esta disponible, el flujo principal de creacion debe continuar.
      }
      setIsCreateOpen(false);
      window.location.hash = `#/inventory/implementos/${created.id}`;
    } catch (error) {
      throw error;
    } finally {
      setSaving(false);
    }
  }

  async function handleUpdate(payload: { id: number; name: string; categoryId: number | null; locationId: number }) {
    setSaving(true);
    setError(null);
    setSuccess(null);

    try {
      await updateImplement(payload.id, {
        name: payload.name,
        category_id: payload.categoryId,
        location_id: payload.locationId,
      });
      await refreshImplements();
      setSuccess("Implemento actualizado correctamente.");
      setEditing(null);
    } catch (error) {
      setError(getErrorMessage(error, "No se pudo actualizar el implemento."));
    } finally {
      setSaving(false);
    }
  }

  return (
    <InventoryLayout activeSection="items">
      <section className="content-header">
        <div>
          <h1>Inventario</h1>
          <p>Alta de producto.</p>
        </div>

        <div className="content-header__actions">
          <button type="button" className="button" onClick={() => setIsCreateOpen(true)}>
            <Plus size={16} />
            Nuevo implemento
          </button>
        </div>
      </section>

      <section className="panel">
        <div className="panel__head">
          <div>
            <h2>Implementos</h2>
            <p>Usa "Nuevo implemento" para crear un producto con categoria y ubicacion obligatorias.</p>
          </div>
        </div>

        <div className="catalog-filters">
          <div className={filters.categoryId != null ? "catalog-filters__item catalog-filters__item--active" : "catalog-filters__item"}>
            <label htmlFor="catalog-filter-category">Categoria</label>
            <select
              id="catalog-filter-category"
              value={filters.categoryId == null ? "" : String(filters.categoryId)}
              onChange={(event) =>
                setFilters((prev) => ({
                  ...prev,
                  categoryId: event.target.value ? Number(event.target.value) : null,
                }))
              }
              disabled={categoriesLoading || categoryOptions.length === 0}
            >
              {categoryOptions.length > 0 ? (
                <>
                  <option value="">Todas las categorias</option>
                  {categoryOptions.map((category) => (
                    <option key={category.id} value={String(category.id)}>
                      {category.name}
                    </option>
                  ))}
                </>
              ) : (
                <option value="">Sin categorias</option>
              )}
            </select>
            {filters.categoryId != null ? <span className="filter-badge">Filtro activo</span> : null}
          </div>

          {isCoordinator ? (
            <div className="catalog-filters__item">
              <label htmlFor="catalog-filter-status">Estado stock</label>
              <select
                id="catalog-filter-status"
                value={filters.stockStatus ?? "all"}
                onChange={(event) =>
                  setFilters((prev) => ({
                    ...prev,
                    stockStatus: event.target.value as ImplementStockFilterStatus,
                  }))
                }
              >
                <option value="all">Todos</option>
                <option value="with_stock">Con stock</option>
                <option value="without_stock">Sin stock</option>
                <option value="low_stock">Stock bajo minimo</option>
              </select>
            </div>
          ) : null}

          <div className="catalog-filters__item catalog-filters__item--search">
            <label htmlFor="catalog-filter-name">Buscar</label>
            <input
              id="catalog-filter-name"
              type="search"
              placeholder="Buscar por nombre..."
              value={filters.name ?? ""}
              onChange={(event) =>
                setFilters((prev) => ({
                  ...prev,
                  name: event.target.value,
                }))
              }
            />
          </div>
        </div>

        <div className="catalog-filters__summary">
          <p>
            Mostrando <strong>{implementos.length}</strong> de <strong>{totalImplements}</strong> implementos
          </p>
          {hasActiveFilters ? (
            <button type="button" className="button button--ghost button--table" onClick={clearFilters}>
              Limpiar filtros
            </button>
          ) : null}
        </div>

        {loading ? <div className="field-hint">Cargando implementos...</div> : null}
        {error ? <div className="error-banner">{error}</div> : null}
        {success ? <div className="success-banner">{success}</div> : null}

        {implementos.length === 0 && !loading ? (
          <div className="empty-state">No se encontraron implementos con los filtros aplicados</div>
        ) : null}

        <div className="table-wrapper">
          <table className="category-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Nombre</th>
                <th>Categoria</th>
                <th>Ubicacion</th>
                <th>Acciones</th>
              </tr>
            </thead>
            <tbody>
              {implementos.map((row) => (
                <tr key={row.id}>
                  <td>{row.id}</td>
                  <td>{row.name}</td>
                  <td>
                    {row.category
                      ? `${row.category.name}${row.category.active ? "" : " [Categoria inactiva]"}`
                      : "Sin categoria"}
                  </td>
                  <td>{row.location ? row.location.name : "Sin ubicacion"}</td>
                  <td>
                    <div className="table-actions">
                      <button
                        type="button"
                        className="button button--table button--ghost"
                        onClick={() => setEditing(row)}
                      >
                        Editar
                      </button>
                      <a className="button button--table button--ghost" href={`#/inventory/implementos/${row.id}`}>
                        Ver ficha
                      </a>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>

      <ImplementFormModal
        isOpen={isCreateOpen}
        saving={saving}
        onClose={() => setIsCreateOpen(false)}
        onSubmit={handleSubmit}
      />

      <ImplementEditModal
        implement={editing}
        isOpen={Boolean(editing)}
        saving={saving}
        onClose={() => setEditing(null)}
        onSubmit={handleUpdate}
      />
    </InventoryLayout>
  );
}

function getUserRoleFromToken(): UserRole {
  const rawToken = window.localStorage.getItem("access_token");
  if (!rawToken) {
    return "UNKNOWN";
  }

  try {
    const payloadPart = rawToken.split(".")[1];
    if (!payloadPart) {
      return "UNKNOWN";
    }

    const normalized = payloadPart.replace(/-/g, "+").replace(/_/g, "/");
    const jsonPayload = decodeURIComponent(
      atob(normalized)
        .split("")
        .map((char) => `%${(`00${char.charCodeAt(0).toString(16)}`).slice(-2)}`)
        .join(""),
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
