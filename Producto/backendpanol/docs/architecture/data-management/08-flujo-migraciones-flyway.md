# Flujo Completo de Migraciones (Flyway)

Este documento define el flujo estándar para crear, revisar, desplegar y validar migraciones SQL en el proyecto.

## Objetivo

- Evitar cambios manuales en producción.
- Asegurar trazabilidad (quién cambió qué y cuándo).
- Reducir riesgo de caída por cambios de esquema.
- Mantener compatibilidad entre código y base de datos en cada release.

## Principios

- Migraciones **forward-only** (`Vn__*.sql`), sin editar versiones ya aplicadas.
- Toda migración viaja en PR junto al código que la consume.
- Cada cambio de esquema debe ser compatible con el estado anterior (estrategia expand/contract cuando aplique).
- Si hay dual-key (ej. `id` + `uuid`), primero expandir, luego migrar uso, después contraer.

## Flujo Normal (end-to-end)

1. **Diseño en rama de feature**
- Crear nuevo script en `src/main/resources/db/migration/` con nombre `V{n}__descripcion.sql`.
- Incluir `IF EXISTS / IF NOT EXISTS` cuando sea necesario para robustez entre ambientes.
- Si hay backfill, dejarlo explícito en el script.

2. **Prueba local**
- Levantar backend contra BD local.
- Verificar que Flyway aplique la migración al iniciar.
- Probar:
  - BD limpia.
  - BD con datos existentes.
- Validar que el backend sigue operativo (login, endpoints críticos).

3. **Pull Request**
- Incluir en PR:
  - SQL de migración.
  - cambios backend/frontend relacionados.
  - plan de rollback.
  - validaciones post-migración (queries/checks).
- Review técnico de DB + backend.

4. **Merge a rama principal**
- Al mergear, la migración queda versionada y lista para despliegue.

5. **Deploy por ambiente (dev -> staging -> prod)**
- El backend arranca.
- Flyway detecta scripts pendientes en `flyway_schema_history`.
- Aplica en orden de versión.
- Si falla una migración, el deploy se considera fallido y se detiene.

6. **Validación post-deploy**
- Revisar logs de arranque Flyway.
- Ejecutar checks de integridad definidos (conteos, nulos, orphan rows, etc.).
- Validar flujos funcionales críticos de negocio.

7. **Monitoreo**
- Seguir métricas de errores API, latencia y locks DB.
- En migraciones por fases, medir adopción (ej. uso de UUID vs ID legado).

## Cuándo se ejecuta una migración

- Regla general: **se ejecuta al desplegar**, después del merge, al iniciar el backend del ambiente objetivo.
- No se recomienda ejecutar SQL manual directo en producción fuera del pipeline, salvo contingencia aprobada.

## Estrategia recomendada de releases (Expand / Migrate / Contract)

1. **Expand**
- Agregar nuevas columnas/tablas/índices sin romper lo existente.

2. **Migrate**
- Cambiar backend/frontend para escribir y leer el nuevo esquema.
- Mantener compatibilidad temporal (dual-write/dual-read si aplica).

3. **Contract**
- Cuando todo el tráfico usa el nuevo esquema, remover legacy (columnas, FKs, claims, endpoints).

## Checklist mínimo por migración

- Script versionado y con nombre correcto.
- No modifica migraciones antiguas ya ejecutadas.
- Ejecuta en local sin errores.
- Incluye validación de integridad.
- Incluye plan de rollback operativo.
- PR aprobado por al menos 1 revisor técnico.

## Rollback: qué sí y qué no

- **Sí**: rollback de aplicación (volver a versión anterior) si la migración fue expandible y backward-compatible.
- **No ideal**: rollback destructivo de esquema en caliente.
- Para cambios críticos: usar backup/snapshot previo y plan de recuperación probado.

## Ejemplo aplicado al caso UUID

1. Deploy 1: migraciones de expansión (`uuid`, `*_uuid`, índices, FKs paralelas, triggers de sync).
2. Deploy 2: backend/frontend consumen UUID como principal; `id` queda legado temporal.
3. Deploy 3: corte final (retiro de legado) con ventana controlada y validaciones reforzadas.
