import { useEffect, useMemo, useState } from "react";
import type { FormEvent } from "react";
import { fetchActiveCategories } from "../../services/activeCategoryService";
import { getErrorMessage } from "../../services/apiClient";
import type { ActiveCategoryOption } from "../../types/categoryActive";

interface ImplementFormModalProps {
  isOpen: boolean;
  saving: boolean;
  onClose: () => void;
  onSubmit: (payload: { name: string; categoryId: number | null }) => Promise<void>;
}

export function ImplementFormModal({ isOpen, saving, onClose, onSubmit }: ImplementFormModalProps) {
  const [name, setName] = useState("");
  const [categoryIdRaw, setCategoryIdRaw] = useState<string>("");

  const [categories, setCategories] = useState<ActiveCategoryOption[]>([]);
  const [loadingCategories, setLoadingCategories] = useState(false);
  const [categoriesError, setCategoriesError] = useState<string | null>(null);

  useEffect(() => {
    if (!isOpen) {
      return;
    }

    setName("");
    setCategoryIdRaw("");
    setCategories([]);
    setCategoriesError(null);
    setLoadingCategories(true);

    fetchActiveCategories()
      .then((result) => setCategories(result))
      .catch((error) => {
        setCategoriesError(getErrorMessage(error, "No se pudo cargar las categorias."));
      })
      .finally(() => setLoadingCategories(false));
  }, [isOpen]);

  const isSelectDisabled = useMemo(() => {
    if (loadingCategories) {
      return true;
    }
    if (categoriesError) {
      return true;
    }
    return categories.length === 0;
  }, [categories.length, categoriesError, loadingCategories]);

  const emptyText = useMemo(() => {
    if (loadingCategories) {
      return "Cargando categorias...";
    }
    if (categoriesError) {
      return categoriesError;
    }
    if (categories.length === 0) {
      return "No hay categorias disponibles";
    }
    return null;
  }, [categories.length, categoriesError, loadingCategories]);

  if (!isOpen) {
    return null;
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    const normalizedName = name.trim();
    const categoryId = categoryIdRaw.trim() ? Number(categoryIdRaw) : null;

    await onSubmit({ name: normalizedName, categoryId: Number.isFinite(categoryId) ? categoryId : null });
  }

  return (
    <div className="modal-overlay" role="dialog" aria-modal="true">
      <div className="modal">
        <h3>Nuevo implemento</h3>
        <p>Completa la informacion del producto. La categoria puede quedar vacia.</p>

        <form onSubmit={handleSubmit}>
          <label htmlFor="implement-name">Nombre</label>
          <input
            id="implement-name"
            value={name}
            onChange={(event) => setName(event.target.value)}
            placeholder="Ej: Guantes latex"
            maxLength={100}
            required
          />

          <label htmlFor="implement-category">Categoria</label>
          <select
            id="implement-category"
            value={categoryIdRaw}
            onChange={(event) => setCategoryIdRaw(event.target.value)}
            disabled={isSelectDisabled}
          >
            {loadingCategories ? (
              <option value="">Cargando categorias...</option>
            ) : categories.length === 0 ? (
              <option value="">No hay categorias disponibles</option>
            ) : (
              <>
                <option value="">Sin categoria</option>
                {categories.map((category) => (
                  <option key={category.id} value={String(category.id)}>
                    {category.name}
                  </option>
                ))}
              </>
            )}
          </select>

          {emptyText && categories.length === 0 && !loadingCategories ? (
            <p className="field-hint">{emptyText}</p>
          ) : null}
          {categoriesError ? <p className="field-error">{categoriesError}</p> : null}

          <div className="modal-actions">
            <button type="button" className="button button--ghost" onClick={onClose}>
              Cancelar
            </button>
            <button type="submit" className="button" disabled={saving || name.trim().length === 0}>
              {saving ? "Guardando..." : "Guardar"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
