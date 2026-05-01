import { useEffect, useMemo, useState } from "react";
import { InventoryLayout } from "../components/layout/InventoryLayout";
import { ImplementEditModal } from "../components/implements/ImplementEditModal";
import { getErrorMessage } from "../services/apiClient";
import { fetchImplementById, updateImplement } from "../services/implementService";
import { fetchLocations } from "../services/locationService";
import { addStockEntry, applyStockMovement, fetchImplementStock, updateIndividualState } from "../services/stockService";
import type { ImplementDetail } from "../types/implement";
import type { LocationOption } from "../types/location";
import type { IndividualItem, StockMovementType } from "../types/stock";
import { getUserRoleFromToken, type UserRole } from "../utils/auth";

const ITEM_TYPE_LABELS: Record<"consumable" | "reusable" | "individual", string> = {
  consumable: "Consumible",
  reusable: "Reutilizable",
  individual: "Individual",
};

const MOVEMENT_OPTIONS: { value: StockMovementType; label: string }[] = [
  { value: "increase_available", label: "Aumentar disponible" },
  { value: "decrease_available", label: "Disminuir disponible" },
  { value: "reserve", label: "Reservar" },
  { value: "release_reserve", label: "Liberar reserva" },
  { value: "loan", label: "Prestar" },
  { value: "return", label: "Devolver" },
  { value: "damage", label: "Marcar dañado" },
  { value: "repair", label: "Reparar" },
];

export function InventoryItemDetailPage({ implementId }: { implementId: number }) {
  const [implement, setImplement] = useState<ImplementDetail | null>(null);
  const [stockDetail, setStockDetail] = useState<any>(null);
  const [loading, setLoading] = useState(false);
  const [stockLoading, setStockLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [stockError, setStockError] = useState<string | null>(null);
  const [isEditing, setIsEditing] = useState(false);
  const [isAttributesEditing, setIsAttributesEditing] = useState(false);
  const [isStockEditing, setIsStockEditing] = useState(false);

  const [userRole, setUserRole] = useState<UserRole>("UNKNOWN");
  const isDocente = userRole === "DOCENTE";

  const [locations, setLocations] = useState<LocationOption[]>([]);
  const [loadingLocations, setLoadingLocations] = useState(false);
  const [locationsError, setLocationsError] = useState<string | null>(null);
  const [selectedLocationId, setSelectedLocationId] = useState<string>("");
  const [savingLocation, setSavingLocation] = useState(false);
  const [locationSaveError, setLocationSaveError] = useState<string | null>(null);

  const [entryQuantity, setEntryQuantity] = useState("1");
  const [assetCodesRaw, setAssetCodesRaw] = useState("");
  const [movementType, setMovementType] = useState<StockMovementType>("reserve");
  const [movementQuantity, setMovementQuantity] = useState("1");
  const [movementIndividualIds, setMovementIndividualIds] = useState("");
  const [stockBusy, setStockBusy] = useState(false);

  useEffect(() => {
    setLoading(true);
    setError(null);
    setSuccess(null);
    setUserRole(getUserRoleFromToken());

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
    setStockLoading(true);
    setStockError(null);

    fetchImplementStock(implementId)
      .then(setStockDetail)
      .catch((requestError) => setStockError(getErrorMessage(requestError, "No se pudo cargar el stock.")))
      .finally(() => setStockLoading(false));
  }, [implementId]);

  useEffect(() => {
    setLoadingLocations(true);
    fetchLocations()
      .then((result) => setLocations(result))
      .catch((requestError) => setLocationsError(getErrorMessage(requestError, "No se pudo cargar las ubicaciones.")))
      .finally(() => setLoadingLocations(false));
  }, []);

  const canSaveLocationChange = useMemo(() => {
    if (!implement || !isAttributesEditing) return false;
    const nextLocationId = selectedLocationId.trim() ? Number(selectedLocationId) : NaN;
    return Number.isFinite(nextLocationId) && nextLocationId !== implement.locationId && !savingLocation && !loadingLocations;
  }, [implement, isAttributesEditing, selectedLocationId, savingLocation, loadingLocations]);

  async function refreshStock() {
    const detail = await fetchImplementStock(implementId);
    setStockDetail(detail);
  }

  async function handleSaveLocation() {
    if (!implement) return;
    const nextLocationId = selectedLocationId.trim() ? Number(selectedLocationId) : NaN;
    if (!Number.isFinite(nextLocationId)) {
      setLocationSaveError("Debes seleccionar una ubicacion.");
      return;
    }

    setSavingLocation(true);
    try {
      const updated = await updateImplement(implementId, {
        name: implement.name,
        category_id: implement.categoryId,
        location_id: nextLocationId,
        item_type: implement.item_type!,
        description: implement.description,
        barcode: implement.barcode,
        img_url: implement.img_url,
        min_stock: implement.min_stock ?? 1,
        observations: implement.observations,
      });
      setImplement(updated);
      setIsAttributesEditing(false);
      setSuccess("Ubicacion actualizada correctamente.");
      await refreshStock();
    } catch (requestError) {
      setLocationSaveError(getErrorMessage(requestError, "No se pudo actualizar la ubicacion."));
    } finally {
      setSavingLocation(false);
    }
  }

  async function handleAddEntry() {
    setStockBusy(true);
    setStockError(null);
    try {
      const payload: { quantity: number; asset_codes?: string[] } = { quantity: Number(entryQuantity) };
      if (implement?.item_type === "individual") {
        payload.asset_codes = assetCodesRaw.split("\n").map((line) => line.trim()).filter(Boolean);
      }
      setStockDetail(await addStockEntry(implementId, payload));
      setSuccess("Ingreso de stock registrado.");
    } catch (requestError) {
      setStockError(getErrorMessage(requestError, "No se pudo registrar el ingreso de stock."));
    } finally {
      setStockBusy(false);
    }
  }

  async function handleMovement() {
    setStockBusy(true);
    setStockError(null);
    try {
      const payload: any = { movement_type: movementType };
      if (implement?.item_type === "individual") {
        payload.individual_ids = movementIndividualIds.split(",").map((v) => Number(v.trim())).filter((v) => Number.isFinite(v) && v > 0);
      } else {
        payload.quantity = Number(movementQuantity);
      }
      setStockDetail(await applyStockMovement(implementId, payload));
      setSuccess("Movimiento de stock aplicado.");
    } catch (requestError) {
      setStockError(getErrorMessage(requestError, "No se pudo aplicar el movimiento de stock."));
    } finally {
      setStockBusy(false);
    }
  }

  async function handleMarkIndividualAvailable(individual: IndividualItem) {
    setStockBusy(true);
    try {
      setStockDetail(await updateIndividualState(implementId, individual.id, { status: "available", condition: "good" }));
      setSuccess(`Individual ${individual.asset_code} actualizado.`);
    } catch (requestError) {
      setStockError(getErrorMessage(requestError, "No se pudo actualizar el individual."));
    } finally {
      setStockBusy(false);
    }
  }

  return (
    <InventoryLayout activeSection="items">
      <section className="content-header">
        <div>
          <h1>Detalle de implemento</h1>
          <p>Inventario / Ficha completa</p>
        </div>
        <div className="content-header__actions">
          <a className="button button--ghost" href="#/inventory/implementos">Volver al listado</a>
          {!isDocente && (
            <>
              <button type="button" className="button" onClick={() => setIsEditing(true)} disabled={loading || !implement}>Editar implemento</button>
              <button type="button" className="button button--ghost" onClick={() => setIsStockEditing((c) => !c)} disabled={loading || !implement}>{isStockEditing ? "Cerrar ajuste stock" : "Ajustar stock"}</button>
            </>
          )}
        </div>
      </section>

      <section className="panel">
        {loading ? <div className="field-hint">Cargando ficha...</div> : null}
        {error ? <div className="error-banner">{error}</div> : null}
        {success ? <div className="success-banner">{success}</div> : null}
        {stockError ? <div className="error-banner">{stockError}</div> : null}

        {implement && !loading ? (
          <div className="implement-detail-layout">
            <div className="implement-left-column">
              <article className="detail-card detail-card--relative">
                <button type="button" className="card-edit-btn" onClick={() => setIsAttributesEditing((c) => !c)}>?</button>
                <div className="implement-hero-detail">
                  <div className="implement-hero">
                    <img src={implement.img_url ?? "https://placehold.co/420x260/e9edf5/4d6284?text=Sin+imagen"} alt={implement.name} className="implement-hero__img" />
                    <button type="button" className="button button--ghost button--sm" disabled>Ver imagen</button>
                  </div>
                  <div>
                    <h2>{implement.name}</h2>
                    <div className="implement-meta-grid">
                      <p>
                        <strong>Categoria:</strong> {implement.category?.name ?? "Sin categoria"}
                        {implement.category && !implement.category.active && (
                          <span className="badge badge--danger" style={{ marginLeft: "8px" }}>Inactiva</span>
                        )}
                      </p>
                      <p><strong>Estado:</strong> {implement.active ? "Activo" : "Inactivo"}</p>
                      <p><strong>Fecha de ingreso:</strong> {implement.createdAt ? new Date(implement.createdAt).toLocaleDateString() : "-"}</p>
                      <p><strong>Tipo:</strong> {implement.item_type ? ITEM_TYPE_LABELS[implement.item_type] : "Sin tipo"}</p>
                      <p><strong>Codigo de barras:</strong> {implement.barcode ?? "No informado"}</p>
                      <p className="implement-meta-grid__full"><strong>Ubicacion principal:</strong> {!isAttributesEditing ? (implement.display_location ?? implement.location?.name ?? "Sin ubicacion") : null}
                        {isAttributesEditing ? (
                          <span className="location-selector">
                            <select value={selectedLocationId} onChange={(e) => setSelectedLocationId(e.target.value)} disabled={loadingLocations}>
                              <option value="" disabled>Selecciona una ubicacion</option>
                              {locations.map((l) => <option key={l.id} value={String(l.id)}>{l.name}</option>)}
                            </select>
                            <button type="button" className="button button--sm" onClick={handleSaveLocation} disabled={!canSaveLocationChange}>{savingLocation ? "Guardando..." : "Guardar"}</button>
                          </span>
                        ) : null}
                      </p>
                      <p className="implement-meta-grid__full"><strong>Observaciones:</strong> {implement.observations ?? "Sin observaciones"}</p>
                    </div>
                    {locationsError ? <p className="field-error">{locationsError}</p> : null}
                    {locationSaveError ? <p className="field-error">{locationSaveError}</p> : null}
                  </div>
                </div>
              </article>

              {isDocente ? (
                <section className="stock-kpi-grid stock-kpi-grid--detail">
                  <article className="stock-kpi stock-kpi--available">
                    <span>Disponibilidad</span>
                    <strong style={{ fontSize: "1.5rem" }}>
                      {implement.stock?.available_display ?? (implement.stock?.available && implement.stock.available > 0 ? "Disponible" : "No disponible")}
                    </strong>
                    <small>Estado actual</small>
                  </article>
                </section>
              ) : (
                <>
                  {stockLoading ? <p className="field-hint">Cargando stock...</p> : null}
                  {stockDetail ? (
                    <section className="stock-kpi-grid stock-kpi-grid--detail">
                      <article className="stock-kpi stock-kpi--available"><span>Disponibles</span><strong>{stockDetail.stock.available}</strong><small>Unidades listas para uso</small></article>
                      <article className="stock-kpi stock-kpi--loaned"><span>Prestados</span><strong>{stockDetail.stock.loaned}</strong><small>Actualmente en préstamo</small></article>
                      <article className="stock-kpi stock-kpi--warn"><span>Reservados</span><strong>{stockDetail.stock.reserved}</strong><small>Reservas activas</small></article>
                      <article className="stock-kpi stock-kpi--danger"><span>Dañados</span><strong>{stockDetail.stock.damaged}</strong><small>Requieren revisión</small></article>
                      <article className="stock-kpi stock-kpi--min"><span>Stock mínimo</span><strong>{stockDetail.stock.min_stock}</strong><small>Nivel mínimo configurado</small></article>
                    </section>
                  ) : null}
                </>
              )}

              {!isDocente && isStockEditing ? (
                <article className="detail-card">
                  <h3>Ajustes de stock</h3>
                  <div className="stock-actions-grid">
                    <div>
                      <h4>Ingreso</h4>
                      <input type="number" min={1} value={entryQuantity} onChange={(event) => setEntryQuantity(event.target.value)} placeholder="Cantidad" />
                      {implement.item_type === "individual" ? <textarea value={assetCodesRaw} onChange={(event) => setAssetCodesRaw(event.target.value)} placeholder="Un asset_code por linea" /> : null}
                      <button type="button" className="button button--sm" disabled={stockBusy} onClick={handleAddEntry}>{stockBusy ? "Procesando..." : "Registrar ingreso"}</button>
                    </div>
                    <div>
                      <h4>Movimiento</h4>
                      <select value={movementType} onChange={(event) => setMovementType(event.target.value as StockMovementType)}>{MOVEMENT_OPTIONS.map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}</select>
                      {implement.item_type === "individual"
                        ? <input type="text" value={movementIndividualIds} onChange={(event) => setMovementIndividualIds(event.target.value)} placeholder="IDs individuales (1,2,3)" />
                        : <input type="number" min={1} value={movementQuantity} onChange={(event) => setMovementQuantity(event.target.value)} placeholder="Cantidad" />}
                      <button type="button" className="button button--sm" disabled={stockBusy} onClick={handleMovement}>{stockBusy ? "Procesando..." : "Aplicar movimiento"}</button>
                    </div>
                  </div>
                </article>
              ) : null}

              {!isDocente && implement.item_type === "individual" ? (
                <article className="detail-card">
                  <h3>Unidades asociadas</h3>
                  <div className="table-wrapper">
                    <table className="category-table">
                      <thead>
                        <tr><th>ID individual</th><th>Codigo</th><th>Estado</th><th>Condicion</th><th>Ubicacion</th><th>Accion</th></tr>
                      </thead>
                      <tbody>
                        {(stockDetail?.individuals ?? []).map((individual: IndividualItem) => (
                          <tr key={individual.id}>
                            <td>{individual.id}</td>
                            <td>{individual.asset_code}</td>
                            <td>{individual.status}</td>
                            <td>{individual.condition}</td>
                            <td>{individual.current_location_id ?? "-"}</td>
                            <td><button type="button" className="button button--table" disabled={stockBusy || individual.status === "available"} onClick={() => handleMarkIndividualAvailable(individual)}>Marcar disponible</button></td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </article>
              ) : null}
            </div>

            <aside className="implement-right-column">
              <article className="detail-card">
                <h3>Descripcion</h3>
                <p>{implement.description ?? "Sin descripcion"}</p>
              </article>
              <article className="detail-card">
                <h3>Información de inventario</h3>
                <p><strong>Observaciones:</strong> {implement.observations ?? "Sin observaciones"}</p>
                <p><strong>Total stock:</strong> {stockDetail?.stock?.total_stock ?? 0}</p>
                <p><strong>Ultima actualizacion:</strong> {implement.updatedAt ? new Date(implement.updatedAt).toLocaleString() : "-"}</p>
              </article>

              {!isDocente && (
                <article className="detail-card">
                  <h3>Últimos 10 movimientos</h3>
                  <div className="table-wrapper">
                    <table className="category-table">
                      <thead>
                        <tr>
                          <th>Fecha</th>
                          <th>Acción</th>
                          <th>Cantidad</th>
                          <th>Usuario</th>
                        </tr>
                      </thead>
                      <tbody>
                        {!implement.recent_movements || implement.recent_movements.length === 0 ? (
                          <tr><td colSpan={4} style={{ textAlign: "center" }}>No hay movimientos recientes registrados</td></tr>
                        ) : (
                          implement.recent_movements.map((mov) => (
                            <tr key={mov.id}>
                              <td>{new Date(mov.timestamp).toLocaleString()}</td>
                              <td><span className="badge">{mov.action}</span></td>
                              <td>{mov.quantity}</td>
                              <td>{mov.performed_by}</td>
                            </tr>
                          ))
                        )}
                      </tbody>
                    </table>
                  </div>
                </article>
              )}
            </aside>
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
