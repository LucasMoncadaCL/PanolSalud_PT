# Backend Docs

## Módulos actuales

- `modules.catalog.category`: API, aplicación, dominio e infraestructura de categorías.
- `shared.error`: errores de negocio y handler global.
- `bootstrap.config`: configuración de seguridad.

## API de Categorías

Base: `/api/categorias`

- `GET /gestion`
- `GET /selector`
- `GET /{id}/asociaciones`
- `GET /{id}/validar-asignacion-implemento`
- `POST /`
- `PUT /{id}`
- `PATCH /{id}/desactivar?force={false|true}`
- `DELETE /{id}`

## Reglas clave HU-74

- Nombre único (case-insensitive) con validación previa y fallback por unique constraint.
- Desactivación conserva trazabilidad histórica de implementos asociados.
- Eliminación bloqueada si existen implementos asociados.
- `created_at` registrado al crear.

## Seguridad actual para integración front

Para facilitar la fase inicial de UI sin autenticación, `/api/categorias/**` está temporalmente con `permitAll`.

## Errores

Formato uniforme:

```json
{
  "code": "CATEGORY_NAME_DUPLICATE",
  "message": "Ya existe una categoria con el nombre 'Reactivos'",
  "timestamp": "2026-04-26T00:00:00Z"
}
```

## Flyway y jOOQ

- Flyway usa baseline marker en `V1__baseline.sql`.
- jOOQ genera modelos desde PostgreSQL/Supabase usando `JOOQ_DB_*`.
- Comando manual: `./mvnw generate-sources` (o `mvnw.cmd generate-sources` en Windows).
- Los fuentes generados quedan en `target/generated-sources/jooq`.
- Referencia detallada: `docs/ENVIRONMENT.md` (seccion "Ejecucion de jOOQ").
