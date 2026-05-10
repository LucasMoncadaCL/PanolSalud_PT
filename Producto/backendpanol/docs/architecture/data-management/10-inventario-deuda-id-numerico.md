# Inventario de Deuda ID Numérico (para corte UUID-only)

Fecha: 2026-05-10

## Backend: endpoints aún no UUID-only

- `LocationController` (`/api/locations/**`): usa `Integer id`.
- `ImplementController` (`/api/implements/**`): usa `Integer id`.
- `StockController` (`/api/implements/{implementId}/stock/**`): usa `Integer implementId` e `Integer individualId`.
- `InventoryMovementController` (`/api/implements/**`): usa `Integer id`.
- `BarcodeLabelController` (`/api/implements/{implementId}/labels/**`): usa `Integer implementId`.
- `CategoriaController` (`/api/categorias/**`): usa `Integer id`.

## Backend: ya alineado (UUID-ready / v2)

- `AuthV2Controller` (`/api/v2/auth/**`).
- `UserAdminV2Controller` (`/api/v2/users/**`) con `UUID` en path.

## Frontend: deuda pendiente relevante

- `types` y `services` de inventario/categorías/ubicaciones/movimientos usan `number`.
- Rutas hash de inventario navegan por `id` numérico.
- Filtros/listados y detalle de implementos/stock usan claves numéricas.

## Frontend: alineado en este corte parcial

- Login/logout consumen `/api/v2/auth/**`.
- Gestión de usuarios Director consume `/api/v2/users/**` y usa `uuid`.
- Helper de auth sin dependencia operativa de `user_id`.

## Orden recomendado para terminar el corte

1. Implementos + stock (`/api/v2/implements/**`).
2. Categorías y ubicaciones (`/api/v2/categories`, `/api/v2/locations`).
3. Movimientos + etiquetas + detalle individual.
4. Ajuste final de rutas hash y tipos TS a UUID-only.
