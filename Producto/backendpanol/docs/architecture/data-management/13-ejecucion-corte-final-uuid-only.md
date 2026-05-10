# Ejecución Completa: Corte Final UUID-Only

## 1) Objetivo
Completar el paso final para operar sin IDs numéricos públicos, dejando solo UUID en contratos externos (`/api/v2/**`), backend operativo y base de datos.

## 2) Pre-requisitos
1. Backend y frontend en versión que consume solo `v2`.
2. Backup PostgreSQL y export Mongo listos.
3. Ventana de mantenimiento aprobada.
4. Equipo con plan de rollback validado.

## 3) Secuencia recomendada
1. Ejecutar prechecks SQL:
   - `docs/architecture/sql/uuid-precheck.sql`
2. Smoke test app actual (pre-corte).
3. Ejecutar corte SQL:
   - `docs/architecture/sql/uuid-final-cutover.sql`
4. Desplegar backend UUID-only.
5. Desplegar frontend UUID-only.
6. Ejecutar postchecks SQL:
   - `docs/architecture/sql/uuid-postcheck.sql`
7. Ejecutar smoke test final.

## 4) Smoke test mínimo (post deploy)
1. `POST /api/v2/auth/login` (Director/Coordinador).
2. `GET /api/v2/users` (Director).
3. `GET /api/v2/categories/gestion` (Coordinador).
4. `GET /api/v2/locations/management` (Coordinador).
5. `GET /api/v2/implements`.
6. `GET /api/v2/implements/movements`.
7. Verificar que rutas legacy respondan `401/404`:
   - `/api/v1/auth/login`
   - `/api/categorias/gestion`
   - `/api/locations/gestion`
   - `/api/implements`

## 5) Checklist backend
1. No exponer controladores legacy en rutas públicas.
2. `JWT sub` como UUID canónico.
3. Sin fallback a `user_id` numérico en auth context.
4. DTOs `v2` sin `id` numérico de negocio.

## 6) Checklist frontend
1. Servicios solo a `/api/v2/**`.
2. Tipos de IDs de negocio en `string` UUID.
3. Sin `id ?? uuid` fallback en rutas críticas.
4. Manejo defensivo para payloads incompletos (`null`) en vistas.

## 7) Rollback (resumen)
1. Restaurar snapshot/dump PostgreSQL.
2. Revertir backend/frontend a tag pre-corte.
3. Revalidar login y flujos core.

