# Deuda Técnica UUID: Cierre por Prioridad

## P0 (bloquea corte DB definitivo)
1. `UserAdminService`: aún resuelve internamente por `id` en queries y updates.
2. `UuidDomainResolver`: puente UUID->ID sigue siendo obligatorio en casi todo catálogo.
3. Módulos `ImplementService`, `StockService`, `InventoryMovementService`: dominio/repositorio centrado en `Integer`.
4. Repositorios jOOQ de catálogo (`ImplementJooqRepository`, `StockJooqRepository`, `CategoriaJooqRepository`, `LocationJooqRepository`) con PK/FK numéricas.

## P1 (contrato ya v2, pero falta pureza interna)
1. Auth/auditoría siguen persistiendo `actor_user_id` / `target_user_id` numéricos.
2. Barcode/stock/movements v2 dependen de controladores legacy internos.
3. DTOs legacy aún existen y deben eliminarse al finalizar transición.

## P2 (front/docs/endurecimiento)
1. `utils/auth.ts` conserva helper de `getUserIdFromToken()` (debe retirarse si no hay uso).
2. Documentación funcional antigua menciona endpoints `/api/*` no-v2.
3. Contratos docs deben consolidarse en UUID-only.

## Plan de cierre incremental
1. Migrar puertos/repositorios dominio a UUID en backend (sin cambiar funcionalidad).
2. Reemplazar servicios internos para no depender de `UuidDomainResolver` salvo compatibilidad temporal.
3. Eliminar controladores/DTO legacy del classpath público y luego del código.
4. Ejecutar corte SQL (`uuid-final-cutover.sql`) cuando P0 esté resuelto.

