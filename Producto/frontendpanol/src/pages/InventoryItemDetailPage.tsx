import { useEffect, useMemo, useState } from "react";
import { InventoryLayout } from "../components/layout/InventoryLayout";
import { getErrorMessage } from "../services/apiClient";
import { fetchImplementById } from "../services/implementService";
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
  const [showInitialStockHint, setShowInitialStockHint] = useState(false);

  useEffect(() => {
    setLoading(true);
    setError(null);

    fetchImplementById(implementId)
      .then((detail) => setImplement(detail))
      .catch((requestError) => {
        setError(getErrorMessage(requestError, "No se pudo cargar la ficha del producto."));
      })
      .finally(() => setLoading(false));
  }, [implementId]);

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

    return { text: "Sin categoria", inactive: false, muted: true };
  }, [implement]);

  const locationLabel = useMemo(() => {
    if (!implement) {
      return "-";
    }
    if (implement.locationId) {
      return `Ubicacion #${implement.locationId}`;
    }
    return "Sin ubicacion";
  }, [implement]);

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
        </div>
      </section>

      <section className="panel">
        {loading ? <div className="field-hint">Cargando ficha...</div> : null}
        {error ? <div className="error-banner">{error}</div> : null}

        {implement && !loading ? (
          <div className="detail-grid">
            <article className="detail-card">
              <h2>{implement.name}</h2>
              <p>{implement.description ?? "Sin descripcion"}</p>
            </article>

            <article className="detail-card">
              <h3>Atributos</h3>
              <p>
                <strong>Categoria:</strong>{" "}
                <span className={categoryView.muted ? "text-muted" : undefined}>{categoryView.text}</span>{" "}
                {categoryView.inactive ? <span className="badge badge--inactive">[Inactiva]</span> : null}
              </p>
              <p>
                <strong>Tipo:</strong>{" "}
                {implement.item_type ? ITEM_TYPE_LABELS[implement.item_type] : "Sin tipo"}
              </p>
              <p>
                <strong>Ubicacion:</strong> {locationLabel}
              </p>
              <p>
                <strong>Stock minimo:</strong>{" "}
                {implement.min_stock === null ? "No informado" : implement.min_stock}
              </p>
              {showInitialStockHint ? (
                <p className="detail-stock-hint">
                  <strong>Stock disponible:</strong> 0{" "}
                  <span className="badge badge--warn">Ingresa un lote</span>
                </p>
              ) : null}
              <p>
                <strong>Observaciones:</strong> {implement.observations ?? "Sin observaciones"}
              </p>
            </article>
          </div>
        ) : null}
      </section>
    </InventoryLayout>
  );
}
