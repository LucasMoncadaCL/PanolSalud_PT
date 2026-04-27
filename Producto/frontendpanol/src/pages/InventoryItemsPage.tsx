import { useState } from "react";
import { Plus } from "lucide-react";
import { InventoryLayout } from "../components/layout/InventoryLayout";
import { ImplementFormModal } from "../components/implements/ImplementFormModal";
import { createImplement } from "../services/implementService";
import { getErrorMessage } from "../services/apiClient";

export function InventoryItemsPage() {
  const [isOpen, setIsOpen] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  async function handleSubmit(payload: { name: string; categoryId: number | null }) {
    setSaving(true);
    setError(null);
    setSuccess(null);

    try {
      await createImplement({ name: payload.name, category_id: payload.categoryId });
      setSuccess("Implemento creado correctamente.");
      setIsOpen(false);
    } catch (error) {
      setError(getErrorMessage(error, "No se pudo crear el implemento."));
    } finally {
      setSaving(false);
    }
  }

  return (
    <InventoryLayout activeSection="items">
      <section className="content-header">
        <div>
          <h1>Inventario</h1>
          <p>Alta de producto (HU-13).</p>
        </div>

        <div className="content-header__actions">
          <button type="button" className="button" onClick={() => setIsOpen(true)}>
            <Plus size={16} />
            Nuevo implemento
          </button>
        </div>
      </section>

      <section className="panel">
        <div className="panel__head">
          <div>
            <h2>Implementos</h2>
            <p>Usa "Nuevo implemento" para crear un producto con categoria opcional.</p>
          </div>
        </div>

        {error ? <div className="error-banner">{error}</div> : null}
        {success ? <div className="success-banner">{success}</div> : null}

        <p className="empty-state">No hay listado implementado en esta vista.</p>
      </section>

      <ImplementFormModal
        isOpen={isOpen}
        saving={saving}
        onClose={() => setIsOpen(false)}
        onSubmit={handleSubmit}
      />
    </InventoryLayout>
  );
}
