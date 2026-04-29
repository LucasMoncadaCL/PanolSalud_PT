# PSD-115 — Selector de ubicación en ficha de producto (Frontend) — HU-30

Fecha: 2026-04-29

## Objetivo

En la ficha del producto:

- Mostrar la ubicación como campo editable mediante un selector desplegable poblado desde `GET /api/locations`.
- Si `display_location` retorna `"Prestado"`, mostrar ese valor como texto no editable con badge distintivo y **no permitir cambios** mientras existan unidades prestadas.
- Al guardar, consumir `PUT /api/implements/{id}` (HU-14) sin crear endpoints nuevos.

## Implementación

### Vista ficha (`InventoryItemDetailPage`)

- Se incorpora carga de ubicaciones con `fetchLocations()` y se renderiza un `<select>` con botón `Guardar`.
- El guardado llama a `updateImplement(implementId, payload)` reutilizando el `PUT /api/implements/{id}`.
- Si `display_location === "Prestado"`, el selector se reemplaza por texto + badge y el cambio queda bloqueado.

Archivo:

- `Producto/frontendpanol/src/pages/InventoryItemDetailPage.tsx`

### Tipos

- Se añade el campo opcional `display_location` en `ImplementDetail` para soportar el nuevo contrato del backend.

Archivo:

- `Producto/frontendpanol/src/types/implement.ts`

## Validaciones / UX

- El botón `Guardar` se deshabilita mientras se cargan ubicaciones o mientras la petición de guardado está en curso.
- Dado que el `PUT` requiere payload completo, si el implemento no tiene información mínima válida (por ejemplo, `min_stock` inválido), se muestra error indicando que primero debe editarse el producto desde el formulario completo.

## Cómo probar

1) Levantar stack “Producto”.
2) Ir a `http://localhost:18081/#/inventory/implementos/1`.
3) Caso normal (sin préstamos):
   - Cambiar ubicación en el selector y presionar `Guardar`.
   - Esperar mensaje “Ubicación actualizada correctamente.” y que la ficha permanezca en la misma página.
4) Caso prestado:
   - Asegurar que el backend retorne `display_location = "Prestado"` (loaned > 0).
   - Verificar que se muestra badge “Prestado” y no aparece selector editable.

