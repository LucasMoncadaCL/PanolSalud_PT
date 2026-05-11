# 04 - MongoDB: Guia Tecnica (Estado Actual UUID-Only)

## 1. Objetivo

MongoDB se usa para trazabilidad operativa de movimientos de inventario con esquema flexible y alta velocidad de lectura.

## 2. Estado actual en codigo

Coleccion implementada y en uso:
- `inventory_movements`

Colecciones planificadas (no implementadas actualmente en backend):
- `loan_events`
- `notifications`
- `stock_alerts`
- `audit_logs` en MongoDB

Nota: la auditoria de autenticacion/usuarios hoy se persiste en PostgreSQL (`audit_log`), no en MongoDB.

## 3. Modelo actual: `inventory_movements` (append-only)

### Uso
- Trazabilidad historica de movimientos manuales y operativos de inventario.
- Lectura de ultimos movimientos por implemento y listados globales descendentes por fecha.

### Estructura de documento (actual)
- `_id`: `ObjectId` (MongoDB)
- `implement_uuid`: `uuid` (referencia logica a `public.implement.uuid` en SQL)
- `action`: `string` (enum de dominio `MovementAction`)
- `quantity`: `int`
- `performed_by_uuid`: `uuid | null` (usuario que ejecuta accion)
- `timestamp`: `Date`
- `notes`: `string | null`

### Repositorio/queries actuales
- `findTop10ByImplementUuidOrderByTimestampDesc(implementUuid)`
- `findAllByOrderByTimestampDesc()`

## 4. Indices recomendados (alineados a UUID)

- `inventory_movements`: `{ implement_uuid: 1, timestamp: -1 }`
- `inventory_movements`: `{ timestamp: -1 }`
- Opcional para analitica por usuario: `{ performed_by_uuid: 1, timestamp: -1 }`

## 5. Validacion de esquema

- Validar en aplicacion que `implement_uuid` y `performed_by_uuid` (cuando exista) tengan formato UUID.
- Mantener `action` dentro del catalogo de `MovementAction`.
- Opcional recomendado: `JSON Schema` en Mongo con `schema_version`.

## 6. Consistencia con SQL (UUID-only)

- `implement_uuid` debe existir en `public.implement.uuid`.
- `performed_by_uuid` (si no es null) debe existir en `public.user.uuid`.
- Regla operacional: primero se confirma la transaccion principal en SQL y luego se registra evento en Mongo.

## 7. Operacion y observabilidad

- Monitorear crecimiento de `inventory_movements` por volumen diario.
- Revisar latencia de lecturas por implemento (top 10) y globales.
- Mantener backup/restore de Mongo probado para continuidad operativa.

## 8. Roadmap (colecciones futuras)

Si se habilitan colecciones adicionales, deben nacer UUID-only:
- `loan_events.loan_uuid`
- `notifications.user_uuid`, `related_loan_uuid`, `related_implement_uuid`
- `stock_alerts.implement_uuid`
- `audit_logs.actor_user_uuid`, `target_user_uuid`, `entity_uuid`

## 9. Estructura propuesta para colecciones futuras (UUID-only)

### `loan_events` (historial embebido por prestamo)

Uso:
- Timeline de cambios de estado de un prestamo en un solo documento.

Documento sugerido:
- `_id`: `ObjectId`
- `loan_uuid`: `uuid` (UNIQUE, referencia logica a SQL)
- `status_history`: `Array<subdocument>`
  - `from_status`: `string`
  - `to_status`: `string`
  - `changed_at`: `Date`
  - `changed_by_uuid`: `uuid | null`
  - `comment`: `string | null`
- `created_at`: `Date`
- `updated_at`: `Date`

Indices recomendados:
- `{ loan_uuid: 1 }` UNIQUE
- `{ "status_history.changed_at": -1 }` (opcional para analitica)

### `notifications` (bandeja por usuario)

Uso:
- Notificaciones de eventos operativos y funcionales para usuarios.

Documento sugerido:
- `_id`: `ObjectId`
- `user_uuid`: `uuid`
- `type`: `string`
- `message`: `string`
- `read`: `boolean`
- `created_at`: `Date`
- `related_loan_uuid`: `uuid | null`
- `related_implement_uuid`: `uuid | null`
- `metadata`: `Object | null`

Indices recomendados:
- `{ user_uuid: 1, read: 1, created_at: -1 }`
- `{ related_loan_uuid: 1 }` (sparse)
- `{ related_implement_uuid: 1 }` (sparse)

### `stock_alerts` (alertas de stock)

Uso:
- Alertas activas e historicas de bajo stock y resolucion operativa.

Documento sugerido:
- `_id`: `ObjectId`
- `implement_uuid`: `uuid`
- `current_stock`: `int`
- `min_stock`: `int`
- `resolved`: `boolean`
- `created_at`: `Date`
- `resolved_at`: `Date | null`
- `resolved_by_uuid`: `uuid | null`

Indices recomendados:
- `{ implement_uuid: 1, resolved: 1, created_at: -1 }`
- `{ resolved: 1, created_at: -1 }`

### `audit_logs` (append-only en Mongo, opcional)

Uso:
- Auditoria funcional/tactica orientada a consulta historica de eventos.
- Nota: actualmente esta auditoria vive en SQL; esta estructura aplica si se migra/duplica a Mongo.

Documento sugerido:
- `_id`: `ObjectId`
- `actor_user_uuid`: `uuid | null`
- `target_user_uuid`: `uuid | null`
- `action`: `string`
- `entity_type`: `string`
- `entity_uuid`: `uuid | null`
- `details`: `Object | null`
  - `before`: `Object | null`
  - `after`: `Object | null`
  - `meta`: `Object | null`
- `logged_at`: `Date`

Indices recomendados:
- `{ entity_type: 1, entity_uuid: 1, logged_at: -1 }`
- `{ actor_user_uuid: 1, logged_at: -1 }`
- `{ logged_at: -1 }`
