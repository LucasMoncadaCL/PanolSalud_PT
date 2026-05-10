# UUID Cutover Big-Bang: Backup y Rollback

Fecha: 2026-05-10  
Objetivo: ejecutar el corte UUID-only con recuperación garantizada.

## 1) Pre-condiciones

- Rama activa: `release/uuid-cutover`.
- Ventana de mantenimiento aprobada.
- Congelamiento de merges funcionales.
- Responsable técnico de backend, frontend y datos asignado.

## 2) Backup obligatorio (antes del deploy)

1. Snapshot de PostgreSQL (proveedor/infra).
2. Dump lógico PostgreSQL:
```bash
pg_dump -h <host> -U <user> -d <db> -Fc -f pre_uuid_cutover.dump
```
3. Export de colecciones Mongo usadas por eventos/auditoría:
- `inventory_movements`
- `loan_events`
- `audit_logs`
- `notifications`
- `stock_alerts`

## 3) Checklist de release

1. Validar migraciones Flyway pendientes y orden.
2. Ejecutar deploy backend con esquema final UUID-only.
3. Ejecutar deploy frontend UUID-only.
4. Correr smoke test:
- login director/coordinador/docente
- CRUD usuarios director
- inventario (listado, detalle, edición)
- movimientos/stock

## 4) Criterios de rollback inmediato

Disparar rollback completo si ocurre cualquiera:
- falla de migración destructiva.
- >5% de 5xx en endpoints críticos por más de 5 minutos.
- login/auth inoperable.
- corrupción o inconsistencia de datos detectada en checks.

## 5) Procedimiento de rollback

1. Detener tráfico (frontend/backend).
2. Restaurar snapshot PostgreSQL (o `pg_restore` del dump).
3. Restaurar export Mongo si hubo cambios incompatibles.
4. Revertir backend/frontend a versión previa estable.
5. Validar smoke test previo (v1).

## 6) Validaciones post-corte

Ejecutar:
- `migration_check_user_uuid`
- `migration_check_relationship_uuid`
- `fn_validate_uuid_migration()`

Todos los contadores deben ser `0`.
