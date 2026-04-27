# Entorno y Secrets del Backend

Este documento explica como configurar variables y secretos del backend, incluyendo la separacion de entornos de base de datos con `APP_DB_ENV`.

## Selector de entorno de BD

La variable principal es:

- `APP_DB_ENV=docker` -> el backend usa `DB_DOCKER_*`
- `APP_DB_ENV=supabase` -> el backend usa `DB_SUPABASE_*`

Si no se define, el valor por defecto es `docker`.

Importante:

- `APP_DB_ENV` solo decide a que base se conecta la aplicacion.
- No decide si Docker Compose crea o no el contenedor `postgres`.

## Archivos que debes crear al clonar

1. Copiar `.env.local.example` a `.env.local`.
2. Crear `secrets/application-secrets.properties`.
3. Crear `secrets/db_password.txt` (solo si usaras docker compose con postgres local).

## Que contiene cada archivo

### `.env.example`

- Plantilla versionada del backend.
- Muestra todas las variables soportadas.
- No debe contener secretos reales.

### `.env.local.example`

- Plantilla para crear `.env.local`.
- Incluye variables para ambos entornos (`docker` y `supabase`).

### `.env.local`

- Archivo local no versionado.
- Define `APP_DB_ENV` y valores concretos para `DB_DOCKER_*` o `DB_SUPABASE_*`.
- Tambien contiene `JOOQ_DB_*` para codegen.

### `secrets/application-secrets.properties`

- Secretos de runtime de Spring (`spring.config.import`).
- Ejemplo:

```properties
DB_DOCKER_PASSWORD=replace_me
DB_SUPABASE_PASSWORD=replace_me
```

Puedes guardar solo la que corresponda al entorno activo.

### `secrets/db_password.txt`

- Password del contenedor `postgres` en `backendpanol/docker-compose.yaml`.
- Se consume como Docker secret (`POSTGRES_PASSWORD_FILE`).

## Variables de runtime del backend

### Comunes

- `APP_PORT`: puerto HTTP del backend.
- `JWT_ISSUER_URI`: issuer JWT.
- `VITE_SUPABASE_URL`, `VITE_SUPABASE_PUBLISHABLE_KEY`: valores de integracion.
- `APP_SECURITY_ENABLED=true|false`: habilita/deshabilita Spring Security (por defecto `false`). Para habilitar seguridad (JWT + roles), usa `true`.

### Entorno Docker local (`APP_DB_ENV=docker`)

- `DB_DOCKER_HOST`
- `DB_DOCKER_PORT`
- `DB_DOCKER_NAME`
- `DB_DOCKER_USER`
- `DB_DOCKER_PASSWORD`
- `DB_DOCKER_SSL_MODE`

### Entorno Supabase (`APP_DB_ENV=supabase`)

- `DB_SUPABASE_HOST`
- `DB_SUPABASE_PORT`
- `DB_SUPABASE_NAME`
- `DB_SUPABASE_USER`
- `DB_SUPABASE_PASSWORD`
- `DB_SUPABASE_SSL_MODE`

## jOOQ codegen (build-time)

Independiente de `APP_DB_ENV`, jOOQ usa:

- `JOOQ_DB_URL`
- `JOOQ_DB_USER`
- `JOOQ_DB_PASSWORD`

Comando recomendado en PowerShell:

```powershell
.\scripts\generate-jooq.ps1
```

Ese script carga automaticamente `.env.local` y `secrets/application-secrets.properties` antes de ejecutar `mvnw.cmd generate-sources`.

## Estado actual de migraciones y baseline

En este proyecto:

- `V1__baseline.sql` es un baseline marker (no-op), no crea tablas.
- El esquema fuente historico existe en Supabase.
- jOOQ no crea schema; solo genera clases Java desde una BD existente.

Consecuencia:

- Si usas `APP_DB_ENV=supabase`, la app funciona con esquema existente.
- Si usas `APP_DB_ENV=docker` con una BD vacia, no se crean tablas mientras no existan migraciones SQL reales (`V2__*.sql`, `V3__*.sql`, etc.) o un bootstrap inicial.

Recomendacion:

1. Definir un bootstrap inicial para local (dump/schema inicial o primera migracion estructural).
2. Desde ahi, versionar solo cambios incrementales con Flyway.

## Diferencia entre `.env` y `.env.local`

- `backendpanol/.env.local`: entorno local del backend.
- `Producto/.env`: entorno del `docker-compose` de nivel `Producto` (backend + frontend).

En `Producto/.env` tambien puedes setear `APP_DB_ENV` y variables `DB_*` para el contenedor backend del stack completo.

## Flujo recomendado segun entorno

### Usar Postgres Docker local

1. `APP_DB_ENV=docker` en `.env.local`.
2. Completar `DB_DOCKER_*`.
3. Definir `DB_DOCKER_PASSWORD` en `secrets/application-secrets.properties`.
4. Crear `secrets/db_password.txt` con la password de postgres local.

### Usar Supabase real

1. `APP_DB_ENV=supabase` en `.env.local` o `Producto/.env`.
2. Completar `DB_SUPABASE_*`.
3. Definir `DB_SUPABASE_PASSWORD` en `secrets/application-secrets.properties`.

## Que se versiona y que no

Se versiona:

- `.env.example`
- `.env.local.example`
- `secrets/README.md`

No se versiona:

- `.env.local`
- `secrets/application-secrets.properties`
- `secrets/*.txt`
