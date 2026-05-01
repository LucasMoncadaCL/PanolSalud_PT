# Documentación de Cambios: HU-20 — Filtro del Catálogo por Estado de Stock

**ID de Tarea:** PSD-25
**Historia de Usuario:** Como Coordinador de Laboratorio, quiero filtrar el catálogo por estado de stock para ver únicamente los productos con unidades disponibles, reservadas, prestadas, dañadas o bloqueadas.

---

## Estado General

| Subtarea | Estado | Nivel de Cumplimiento |
|----------|--------|-----------------------|
| Subtarea 1 — Backend: Soporte `?stockStatus=` en `GET /api/implements` | ✅ Completada | 100% |
| Subtarea 2 — Frontend: Selector de filtro por estado (solo Coordinador) | ✅ Completada | 100% |
| Subtarea 3 — Frontend: Badge de filtro activo y contador de resultados | ✅ Completada | 100% |

---

## Subtarea 1 — Soporte de parámetro `?stockStatus=` en `GET /api/implements` (Backend)

**Estado:** ✅ Completada — 100%

### Cambios Realizados

#### `StockStatusFilter.java` _(nuevo)_
- Enum de dominio creado con los valores: `AVAILABLE`, `RESERVED`, `LOANED`, `DAMAGED`, `BLOCKED`.
- Método de fábrica `fromValue(String)` con resolución case-insensitive.
- Documentación Javadoc explicando el propósito y los valores válidos.

#### `ImplementRepository.java`
- Firma de `findAllSummaries()` extendida con parámetro `StockStatusFilter stockStatusFilter` (nullable).
- Cuando es `null`, el comportamiento es idéntico al original (sin filtro de stock).

#### `ImplementJooqRepository.java`
- Método `findAllSummaries()` actualizado para construir la condición SQL `WHERE stock.{campo} > 0` dinámicamente.
- Helper privado `resolveStockField(StockStatusFilter)` con `switch` exhaustivo para mapear cada valor del enum al campo jOOQ correspondiente de la tabla `stock`.
- El filtro es combinable con `name` y `categoryId` mediante condición `AND`.
- `BLOCKED` se resuelve con `DSL.field()` dinámico por no tener columna tipada en el codegen actual.

#### `ImplementService.java`
- Método `listar()` extendido con parámetro `StockStatusFilter stockStatusFilter` y propagado al repositorio.

#### `ImplementController.java`
- `GET /api/implements` ahora acepta `@RequestParam(required = false) String stockStatus`.
- Validación de rol: si la seguridad está activa (`authentication != null`) y el usuario no es Coordinador, se lanza `AccessDeniedException` (403).
- Valor inválido retorna `BadRequestException` (400) con mensaje descriptivo de los valores válidos.
- **Dev mode:** cuando `authentication == null` (seguridad desactivada), el filtro se permite sin restricción de rol.
- Helper privado `hasRole(Authentication, String)` extraído para reutilización limpia.

#### `ImplementServiceTest.java`
- Test `listarDebeAplicarFiltrosCombinados()` actualizado para reflejar la nueva firma de tres parámetros.

### Criterios de Aceptación Verificados
- [x] `?stockStatus=available` → retorna solo implementos con `stock.available > 0`
- [x] `?stockStatus=loaned&categoryId=2` → combina ambas condiciones (AND)
- [x] Sin parámetro → comportamiento original intacto
- [x] Valor inválido → `400 Bad Request` con mensaje claro
- [x] Backend compila y tests pasan sin errores

---

## Subtarea 2 — Selector de filtro por estado de stock en catálogo — solo Coordinador (Frontend)

**Estado:** ✅ Completada — 100%

### Cambios Realizados

#### `src/types/implement.ts`
- Tipo `ImplementStockFilterStatus` reemplazado con los 5 valores reales del backend: `"all" | "available" | "reserved" | "loaned" | "damaged" | "blocked"`.
- Constante `STOCK_STATUS_LABELS` agregada: mapa `Record<ImplementStockFilterStatus, string>` con los nombres legibles en español para cada estado.

#### `src/services/implementService.ts`
- `fetchImplements()` ahora envía `?stockStatus=` al backend cuando el valor es distinto de `"all"`.
- Se elimina el filtrado en memoria que existía antes (no más lógica de `with_stock`, `without_stock`, `low_stock` en el cliente).

#### `src/pages/InventoryItemsPage.tsx`
- Import de `STOCK_STATUS_LABELS` agregado.
- `refreshImplements()` simplificado: elimina el bloque de `.filter()` en memoria y pasa `filters.stockStatus` directamente al servicio.
- Selector de "Estado stock" visible exclusivamente para `isCoordinator === true`.
- Opciones actualizadas: `Todos los estados`, `Disponible`, `Reservado`, `Prestado`, `Dañado`, `Bloqueado`.
- El filtro se aplica de forma inmediata al cambiar la selección (sin botón de confirmación).

### Criterios de Aceptación Verificados
- [x] Selector visible únicamente para el rol Coordinador
- [x] Cambio de estado dispara inmediatamente la consulta sin confirmación adicional
- [x] Los tres filtros (nombre, categoría, estado stock) son combinables sin que uno resetee al otro
- [x] Selector oculto para Director y Docente

---

## Subtarea 3 — Badge de filtro activo y contador de resultados (Frontend)

**Estado:** ✅ Completada — 100%

### Cambios Realizados

#### `src/pages/InventoryItemsPage.tsx`
- El contenedor del selector de estado de stock recibe la clase CSS `catalog-filters__item--active` cuando el estado seleccionado es distinto de `"all"`.
- Badge `<span className="filter-badge">` con el nombre legible del estado activo (ej: `stockStatus=loaned` → badge `"Prestado"`), usando `STOCK_STATUS_LABELS`.
- La variable `hasActiveFilters` ya contemplaba `stockStatus !== "all"`, por lo que el botón **"Limpiar filtros"** resetea correctamente los tres filtros incluyendo el de stock.
- El contador `"Mostrando X de Y implementos"` funciona correctamente con los tres filtros combinados ya que el total viene del backend filtrado.

### Criterios de Aceptación Verificados
- [x] Badge visible con nombre legible cuando stockStatus está activo
- [x] Contenedor del filtro resaltado visualmente con clase `--active`
- [x] "Limpiar filtros" resetea también el estado de stock
- [x] Contador de resultados funciona correctamente con combinación de filtros
- [x] Array vacío muestra mensaje de estado vacío existente

---

## Deuda Técnica Registrada

### DT-01 — Campo `blocked` sin field tipado en codegen de jOOQ

**Archivo:** `ImplementJooqRepository.java` — método `resolveStockField()`
**Registrado:** 2026-05-01 — Observación aprobada en revisión de PSD-25

El caso `BLOCKED` del switch usa `DSL.field(DSL.name("stock", "blocked"), Integer.class)` de forma dinámica porque la columna `blocked` de la tabla `stock` no está incluida en el codegen de jOOQ actual. Esto significa que errores de tipeo en el nombre de la columna no serán detectados en tiempo de compilación.

**Acción de resolución:** Cuando se ejecute una nueva generación de código jOOQ (tras agregar la columna al schema de PostgreSQL si aplica), reemplazar la línea dinámica por `STOCK.BLOCKED` (field tipado). El comentario `// TODO` en el código señala exactamente el punto de intervención.

---

## Tests Agregados como Deuda Técnica

### Test (a) — `ImplementServiceTest.java`
`listarConFiltroBlockedDebePropagarsAlRepositorio()`

Verifica que cuando se llama a `listar(null, null, StockStatusFilter.BLOCKED)`, el servicio propaga exactamente ese filtro al repositorio sin mutarlo. Garantiza que la lógica de propagación es consistente con los demás estados.

### Test (b) — `ImplementControllerSecurityTest.java` _(nuevo archivo)_
Tres casos cubriendo la restricción de rol del filtro `?stockStatus=`:

| Test | Rol | Resultado esperado |
|------|-----|--------------------|
| `listarConStockStatusDebeFallarCon403ParaRolDocente` | DOCENTE | 403 Forbidden |
| `listarConStockStatusDebeFallarCon403ParaRolDirector` | DIRECTOR | 403 Forbidden |
| `listarConStockStatusDebePermitirseParaRolCoordinador` | COORDINADOR | 200 OK |

Estos tests usan `@WebMvcTest` + `@WithMockUser` de `spring-security-test` (ya disponible en el `pom.xml`) y garantizan que la restricción de rol funcionará correctamente cuando la seguridad JWT se active en producción (HU-01 Sprint 2).

---


### Filtrado en Backend vs. Frontend
Se migró el filtrado desde el cliente (JavaScript en memoria) hacia el backend (SQL en jOOQ). Esto garantiza:
- **Consistencia:** el paginado futuro funcionará correctamente.
- **Rendimiento:** no se transfieren registros innecesarios por red.
- **Seguridad:** el cliente no puede evadir el filtro.

### Dev Mode — Seguridad Desactivada
Se añadió `APP_SECURITY_ENABLED=false` al `.env` del backend y se corrigió el controlador para que, cuando `authentication == null`, se omita la validación de rol. Esto permite realizar pruebas manuales en Sprint 1 sin token JWT.
