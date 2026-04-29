# PSD-114 — Lógica de ubicación automática “Prestado” (Backend) — HU-30

Fecha: 2026-04-29

## Objetivo

Aplicar una lógica consistente (en backend) para exponer la ubicación “visible” del producto:

- Si `stock.loaned > 0`, el campo `display_location` debe ser `"Prestado"`.
- Si `stock.loaned = 0`, `display_location` debe ser el nombre real de la ubicación.

La lógica se implementa en el backend (Service / DB view) para asegurar consistencia para cualquier consumidor de la API.

## Implementación

### API: `GET /api/implements/{id}`

Se agrega el campo `display_location` en la respuesta del endpoint.

Regla:

- `loaned > 0` ⇒ `"Prestado"`
- en caso contrario ⇒ `location.name`

Archivos:

- `Producto/backendpanol/src/main/java/com/panol_project/backendpanol/modules/catalog/implement/application/ImplementService.java` (`resolveDisplayLocation`)
- `Producto/backendpanol/src/main/java/com/panol_project/backendpanol/modules/catalog/implement/api/ImplementController.java` (mapea `display_location`)
- `Producto/backendpanol/src/main/java/com/panol_project/backendpanol/modules/catalog/implement/api/dto/ImplementResponse.java` (nuevo campo `display_location`)

### DB: `v_stock_summary`

Se versiona la vista con Flyway agregando la columna `display_location` con la misma regla.

Archivo:

- `Producto/backendpanol/src/main/resources/db/migration/V4__v_stock_summary_display_location.sql`

## Contrato esperado (API)

Ejemplo:

```json
{
  "location": { "id": 1, "name": "Por definir" },
  "display_location": "Prestado"
}
```

## Cómo probar

1) Asegura que el implemento tenga `loaned > 0` en `stock`.
2) Consulta:

```powershell
Invoke-RestMethod -Method Get -Uri "http://localhost:18080/api/implements/1"
```

Resultado esperado:

- Con `loaned > 0` ⇒ `display_location = "Prestado"`.
- Con `loaned = 0` ⇒ `display_location = location.name`.

