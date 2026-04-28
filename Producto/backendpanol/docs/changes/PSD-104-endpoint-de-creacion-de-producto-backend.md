# PSD-104 - Endpoint POST /api/implements para registrar nuevo producto (HU-13)

Fecha: 2026-04-27

## Resumen

Se implemento el endpoint `POST /api/implements` para registrar productos con validaciones de negocio, validaciones de contrato y control de acceso por rol.

Se incorporaron los nuevos campos del caso de uso:

- `name` (obligatorio)
- `category_id` (obligatorio)
- `item_type` (obligatorio: `consumable|reusable|individual`)
- `location_id` (obligatorio)
- `description` (opcional)
- `min_stock` (obligatorio, entero positivo)
- `observations` (opcional)

El stock se maneja mediante la tabla `public.stock` (1 fila por implemento). Por ahora el backend solo gestiona el campo `min_stock`; el resto del stock (`total_stock`, `available`, etc.) queda en `0` (por defecto/trigger segun el entorno). Luego de crear el implemento, el backend hace un upsert para dejar `stock.min_stock` con el valor enviado en el request.

## Alcance funcional

### Endpoint

- `POST /api/implements`
- Respuesta esperada: `201 Created` con datos del producto creado.

### Seguridad

- Requiere JWT valido.
- Restringido a rol `COORDINADOR` mediante `@PreAuthorize("hasRole('COORDINADOR')")`.
- Sin JWT -> `401`.
- Con JWT sin rol -> `403`.

### Validaciones de request (400)

- `name`: obligatorio, max 120.
- `category_id`: obligatorio.
- `item_type`: obligatorio y dentro de `consumable|reusable|individual`.
- `location_id`: obligatorio.
- `min_stock`: obligatorio y `> 0`.
- `description`: max 255.
- `observations`: max 500.

Cuando falta `location_id`, el backend responde `400` con mensaje de validacion: `La ubicacion es obligatoria`.

### Reglas de negocio aplicadas

1. Categoria debe existir y estar activa.
2. Nombre unico de producto (case-insensitive) sobre implementos activos (validacion previa + fallback por constraint unico).
3. `item_type` se normaliza a enum de dominio (`consumable|reusable|individual`).
4. Se asegura existencia de `stock` por implemento mediante upsert (`INSERT ... ON CONFLICT (implement_id) DO UPDATE`) al persistir `min_stock`.

## Cambios tecnicos

### API / DTO

- `CreateImplementRequest` extendido con `item_type`, `min_stock`, `observations` y validaciones.
- `ImplementResponse` extendido con `item_type`, `min_stock`, `observations`.

### Aplicacion / Dominio

- `ImplementService.crear(...)` ampliado para:
  - validaciones de categoria y ubicacion,
  - parseo de tipo de implemento,
  - creacion del implemento,
  - actualizacion de `stock.min_stock`.
- Nuevo enum de dominio: `ImplementItemType`.
- `Implemento` ahora incluye `itemType`.

### Infraestructura

- `ImplementRepository` ampliado con:
  - `create(..., itemType, observations)`
  - `updateMinStockByImplementId(...)`
- `ImplementJooqRepository`:
  - persiste `item_type` y `observations` en `implement`.
  - realiza upsert de `stock.min_stock` por `implement_id`.

### Migracion de BD

- Nueva migracion: `V3__implement_observations_column.sql`
  - agrega columna opcional `implement.observations` (`VARCHAR(500)`).

## Archivos modificados

- `src/main/java/com/panol_project/backendpanol/modules/catalog/implement/api/ImplementController.java`
- `src/main/java/com/panol_project/backendpanol/modules/catalog/implement/api/dto/CreateImplementRequest.java`
- `src/main/java/com/panol_project/backendpanol/modules/catalog/implement/api/dto/ImplementResponse.java`
- `src/main/java/com/panol_project/backendpanol/modules/catalog/implement/application/ImplementService.java`
- `src/main/java/com/panol_project/backendpanol/modules/catalog/implement/domain/ImplementRepository.java`
- `src/main/java/com/panol_project/backendpanol/modules/catalog/implement/domain/Implemento.java`
- `src/main/java/com/panol_project/backendpanol/modules/catalog/implement/domain/ImplementItemType.java`
- `src/main/java/com/panol_project/backendpanol/modules/catalog/implement/infrastructure/ImplementJooqRepository.java`
- `src/main/resources/db/migration/V3__implement_observations_column.sql`

## Pruebas

### Unitarias / Seguridad

- `ImplementServiceTest` actualizado al nuevo flujo de creacion.
- `ImplementCreateEndpointSecurityTest` agregado para validar:
  - `401` sin JWT,
  - `403` sin rol coordinador,
  - `201` con rol coordinador,
  - `400` por `location_id` faltante.

### Comando ejecutado

```powershell
.\mvnw.cmd -q "-Dnet.bytebuddy.experimental=true" "-Dtest=ImplementServiceTest,ImplementCreateEndpointSecurityTest" test
```

Nota: en este entorno (Java 25) fue necesario `-Dnet.bytebuddy.experimental=true` para Mockito/ByteBuddy.

## Como validar manualmente

1. Enviar `POST /api/implements` con token de coordinador y payload valido -> `201`.
2. Repetir sin JWT -> `401`.
3. Repetir con JWT sin rol coordinador -> `403`.
4. Omitir `location_id` -> `400` con mensaje `La ubicacion es obligatoria`.
5. Enviar `item_type` invalido -> `400`.
6. Enviar `min_stock` <= 0 -> `400`.
7. Verificar en BD que `stock.min_stock` coincide con request y stock inicial permanece en 0.
