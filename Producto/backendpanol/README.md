# Backend Panol - HU-74 Categorias

Implementa la historia **HU-74** usando Spring Boot + jOOQ + Flyway.

## Alcance funcional

- CRUD de categorias (listar, crear, editar, desactivar, eliminar).
- Solo usuarios con rol `COORDINADOR` pueden gestionar categorias.
- Nombre de categoria unico (case-insensitive) con validacion previa y fallback por constraint unico.
- Una categoria desactivada mantiene su referencia historica en implementos.
- Al desactivar: si existen implementos activos asociados, el backend responde advertencia (conflict) con conteo; se puede confirmar con `force=true`.
- Eliminacion bloqueada cuando existen implementos asociados (activos o inactivos).
- Se registra el momento de creacion (`created_at`).
- Validacion reusable para impedir asignar categoria inactiva al crear/editar implementos.

## Endpoints

Base path: `/api/categorias`

- `GET /api/categorias/gestion` (todas: activas + inactivas)
- `GET /api/categorias/selector` (solo activas)
- `GET /api/categorias/{id}/asociaciones` (conteo de implementos y si se puede eliminar)
- `GET /api/categorias/{id}/validar-asignacion-implemento` (204 si categoria activa)
- `POST /api/categorias`
- `PUT /api/categorias/{id}`
- `PATCH /api/categorias/{id}/desactivar?force=false`
- `DELETE /api/categorias/{id}`

Compatibilidad:

- `GET /api/categorias?incluirInactivas=true|false`

## Formato de errores backend

Todos los errores 4xx/5xx devuelven payload uniforme:

```json
{
  "code": "CATEGORY_NAME_DUPLICATE",
  "message": "Ya existe una categoria con el nombre 'Reactivos'",
  "timestamp": "2026-04-25T16:20:00.000-04:00"
}
```

## Seguridad

- Resource Server JWT (`spring-boot-starter-oauth2-resource-server`).
- Se acepta rol desde claims: `role`, `user_role`, `roles`, `app_metadata.role`, `app_metadata.roles`.
- Rol requerido: `ROLE_COORDINADOR`.

## jOOQ

- Modelos generados desde la base PostgreSQL real (Supabase).
- Comando:

```bash
./mvnw generate-sources
```

Variables requeridas para codegen:

```bash
JOOQ_DB_URL=jdbc:postgresql://aws-1-us-east-1.pooler.supabase.com:5432/postgres?sslmode=require
JOOQ_DB_USER=postgres.ekqbucfoygimzgxupdhm
JOOQ_DB_PASSWORD=tu_password_db
```

## Variables y secrets

1. Copiar `.env.local.example` a `.env.local`.
2. Crear `secrets/db_password.txt` (para docker postgres).
3. Crear `secrets/application-secrets.properties` con secretos backend, por ejemplo:

```properties
DB_PASSWORD=replace_me
```

## Flyway y esquema existente

- Este proyecto asume que el esquema principal ya existe en Supabase.
- `V1__baseline.sql` es intencionalmente no-op (baseline marker).
- `spring.flyway.baseline-on-migrate=true` permite iniciar historial Flyway en una BD no vacia sin recrear tablas existentes.
- Cambios nuevos deben agregarse como migraciones forward-only: `V2__*.sql`, `V3__*.sql`, etc.

## Run local

```bash
./mvnw spring-boot:run
```

## Run con Docker Compose

```bash
docker compose up --build
```

## Supabase entregado

- `VITE_SUPABASE_URL=https://ekqbucfoygimzgxupdhm.supabase.co`
- `VITE_SUPABASE_PUBLISHABLE_KEY=sb_publishable_zz8M0TK5BSRjk00UMl9h6g_qgWycc02`
