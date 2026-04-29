import { useEffect, useMemo, useState } from "react";
import { InventoryLayout } from "../components/layout/InventoryLayout";
import { getErrorMessage } from "../services/apiClient";
import { fetchImplementById, updateImplement } from "../services/implementService";
import { fetchLocations } from "../services/locationService";
import { ImplementEditModal } from "../components/implements/ImplementEditModal";
import type { LocationOption } from "../types/location";
import type { ImplementDetail } from "../types/implement";

const ITEM_TYPE_LABELS: Record<"consumable" | "reusable" | "individual", string> = {
  consumable: "Consumible",
  reusable: "Reutilizable",
  individual: "Individual",
};

export function InventoryItemDetailPage({ implementId }: { implementId: number }) {
  const [implement, setImplement] = useState<ImplementDetail | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [showInitialStockHint, setShowInitialStockHint] = useState(false);
  const [isEditing, setIsEditing] = useState(false);

  const [locations, setLocations] = useState<LocationOption[]>([]);
  const [loadingLocations, setLoadingLocations] = useState(false);
  const [locationsError, setLocationsError] = useState<string | null>(null);
  const [selectedLocationId, setSelectedLocationId] = useState<string>("");
  const [savingLocation, setSavingLocation] = useState(false);
  const [locationSaveError, setLocationSaveError] = useState<string | null>(null);

  useEffect(() => {
    setLoading(true);
    setError(null);
    setSuccess(null);

    fetchImplementById(implementId)
      .then((detail) => {
        setImplement(detail);
        setSelectedLocationId(detail.locationId == null ? "" : String(detail.locationId));
      })
      .catch((requestError) => {
        setError(getErrorMessage(requestError, "No se pudo cargar la ficha del producto."));
      })
      .finally(() => setLoading(false));
  }, [implementId]);

  useEffect(() => {
    setLocations([]);
    setLocationsError(null);
    setLoadingLocations(true);

    fetchLocations()
      .then((result) => setLocations(result))
      .catch((requestError) => setLocationsError(getErrorMessage(requestError, "No se pudo cargar las ubicaciones.")))
      .finally(() => setLoadingLocations(false));
  }, []);

  useEffect(() => {
    try {
      const marker = window.sessionStorage.getItem("inventory.justCreatedImplementId");
      const markerId = marker ? Number(marker) : NaN;
      const isJustCreated = Number.isFinite(markerId) && markerId === implementId;
      setShowInitialStockHint(isJustCreated);
      if (isJustCreated) {
        window.sessionStorage.removeItem("inventory.justCreatedImplementId");
      }
    } catch {
      setShowInitialStockHint(false);
    }
  }, [implementId]);

  const categoryView = useMemo(() => {
    if (!implement) {
      return { text: "-", inactive: false, muted: false };
    }

    if (implement.category) {
      return {
        text: implement.category.name,
        inactive: !implement.category.active,
        muted: false,
      };
    }

    return { text: "Sin categoría", inactive: false, muted: true };
  }, [implement]);

  const locationLabel = useMemo(() => {
    if (!implement) {
      return "-";
    }
    if (implement.display_location) {
      return implement.display_location;
    }
    if (implement.location) {
      return implement.location.name;
    }
    if (implement.locationId) {
      return `Ubicación #${implement.locationId}`;
    }
    return "Sin ubicación";
  }, [implement]);

  const isLoanedLocation = useMemo(() => implement?.display_location === "Prestado", [implement?.display_location]);

  const canSaveLocationChange = useMemo(() => {
    if (!implement) {
      return false;
    }
    if (savingLocation || loading || loadingLocations) {
      return false;
    }
    if (Boolean(locationsError) || Boolean(locationSaveError)) {
      return false;
    }
    if (isLoanedLocation) {
      return false;
    }

    const nextLocationId = selectedLocationId.trim() ? Number(selectedLocationId) : NaN;
    if (!Number.isFinite(nextLocationId)) {
      return false;
    }

    // PUT /api/implements/{id} requiere payload completo.
    if (!implement.name || implement.name.trim().length === 0) {
      return false;
    }
    if (!implement.item_type) {
      return false;
    }
    if (implement.min_stock == null || implement.min_stock <= 0 || !Number.isInteger(implement.min_stock)) {
      return false;
    }
    if (implement.categoryId == null) {
      return false;
    }

    return nextLocationId !== implement.locationId;
  }, [
    implement,
    isLoanedLocation,
    loading,
    loadingLocations,
    locationSaveError,
    locationsError,
    savingLocation,
    selectedLocationId,
  ]);

  async function handleSaveLocation() {
    if (!implement) {
      return;
    }

    setLocationSaveError(null);
    setSuccess(null);

    const nextLocationId = selectedLocationId.trim() ? Number(selectedLocationId) : NaN;
    if (!Number.isFinite(nextLocationId)) {
      setLocationSaveError("Debes seleccionar una ubicación.");
      return;
    }

    if (!implement.item_type) {
      setLocationSaveError("El tipo de implemento no está definido. Edita el producto antes de cambiar la ubicación.");
      return;
    }

    if (implement.min_stock == null || implement.min_stock <= 0 || !Number.isInteger(implement.min_stock)) {
      setLocationSaveError("El stock mínimo del producto es inválido. Edita el producto antes de cambiar la ubicación.");
      return;
    }

    if (implement.categoryId == null) {
      setLocationSaveError("La categoría del producto no está definida. Edita el producto antes de cambiar la ubicación.");
      return;
    }

    setSavingLocation(true);
    try {
      const updated = await updateImplement(implementId, {
        name: implement.name,
        category_id: implement.categoryId,
        location_id: nextLocationId,
        item_type: implement.item_type,
        description: implement.description,
        min_stock: implement.min_stock,
        observations: implement.observations,
      });

      setImplement(updated);
      setSelectedLocationId(updated.locationId == null ? "" : String(updated.locationId));
      setSuccess("Ubicación actualizada correctamente.");
    } catch (requestError) {
      setLocationSaveError(getErrorMessage(requestError, "No se pudo actualizar la ubicación."));
    } finally {
      setSavingLocation(false);
    }
  }

  return (
    <InventoryLayout activeSection="items">
      <section className="content-header">
        <div>
          <h1>Ficha de producto</h1>
          <p>Detalle del implemento registrado.</p>
        </div>
        <div className="content-header__actions">
          <a className="button button--ghost" href="#/inventory/implementos">
            Volver al listado
          </a>
          <button type="button" className="button" onClick={() => setIsEditing(true)} disabled={loading || !implement}>
            Editar
          </button>
        </div>
      </section>

      <section className="panel">
        {loading ? <div className="field-hint">Cargando ficha...</div> : null}
        {error ? <div className="error-banner">{error}</div> : null}
        {success ? <div className="success-banner">{success}</div> : null}

        {implement && !loading ? (
          <div className="detail-grid">
            <article className="detail-card">
              <h2>{implement.name}</h2>
              <p>{implement.description ?? "Sin descripcion"}</p>
            </article>

            <article className="detail-card">
              <h3>Atributos</h3>
              <p>
                <strong>Categoría:</strong>{" "}
                <span className={categoryView.muted ? "text-muted" : undefined}>{categoryView.text}</span>{" "}
                {categoryView.inactive ? <span className="badge badge--inactive">[Inactiva]</span> : null}
              </p>
              <p>
                <strong>Tipo:</strong> {implement.item_type ? ITEM_TYPE_LABELS[implement.item_type] : "Sin tipo"}
              </p>
              <p>
                <strong>Ubicación:</strong>{" "}
                {isLoanedLocation ? (
                  <>
                    {locationLabel} <span className="badge badge--warn">Prestado</span>
                  </>
                ) : (
                  <span className="location-selector">
                    <select
                      value={selectedLocationId}
                      onChange={(event) => {
                        setSelectedLocationId(event.target.value);
                        setLocationSaveError(null);
                      }}
                      disabled={loadingLocations || Boolean(locationsError) || savingLocation}
                      aria-label="Seleccionar ubicación"
                    >
                      <option value="" disabled>
                        Selecciona una ubicación
                      </option>
                      {locations.map((location) => (
                        <option key={location.id} value={String(location.id)}>
                          {location.name}
                        </option>
                      ))}
                    </select>
                    <button type="button" className="button button--sm" onClick={handleSaveLocation} disabled={!canSaveLocationChange}>
                      {savingLocation ? "Guardando..." : "Guardar"}
                    </button>
                  </span>
                )}
              </p>
              {locationsError ? <p className="field-error">{locationsError}</p> : null}
              {locationSaveError ? <p className="field-error">{locationSaveError}</p> : null}
              {!isLoanedLocation && implement.min_stock != null && implement.min_stock <= 0 ? (
                <p className="field-hint">Para cambiar la ubicación, primero edita el producto y define un stock mínimo válido.</p>
              ) : null}
              <p>
                <strong>Stock mínimo:</strong> {implement.min_stock === null ? "No informado" : implement.min_stock}
              </p>
              {showInitialStockHint ? (
                <p className="detail-stock-hint">
                  <strong>Stock disponible:</strong> 0 <span className="badge badge--warn">Ingresa un lote</span>
                </p>
              ) : null}
              <p>
                <strong>Observaciones:</strong> {implement.observations ?? "Sin observaciones"}
              </p>
            </article>
          </div>
        ) : null}
      </section>

      <ImplementEditModal
        implementId={implementId}
        isOpen={isEditing}
        onClose={() => setIsEditing(false)}
        onSaved={(updated) => {
          setImplement(updated);
          setSelectedLocationId(updated.locationId == null ? "" : String(updated.locationId));
          setSuccess("Producto actualizado correctamente.");
        }}
      />
    </InventoryLayout>
  );
}

