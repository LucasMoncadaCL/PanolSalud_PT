# Entorno y Secrets del Backend

Este documento explica que archivos debes crear/configurar al clonar el repositorio, para que sirve cada variable y cual es la diferencia entre `.env` y `.env.local`.

## Resumen rapido post-clone

1. Copiar `backendpanol/.env.local.example` a `backendpanol/.env.local`.
2. Crear `backendpanol/secrets/application-secrets.properties`.
3. Crear `backendpanol/secrets/db_password.txt`.
4. Completar valores sensibles:
   - `DB_PASSWORD` en `application-secrets.properties`.
   - `JOOQ_DB_PASSWORD` en `.env.local` (si usaras codegen jOOQ).
   - Password para Postgres Docker en `db_password.txt` (si usaras docker compose local con Postgres).

## Diferencia entre `.env` y `.env.local`

### `backendpanol/.env.local`

- Uso: variables locales del backend.
- Donde se usa:
  - `docker compose` de `backendpanol` lo carga explicitamente con `env_file: .env.local`.
  - Tambien sirve como referencia para ejecutar backend en local.
- Estado en git: ignorado (`.gitignore`), no se versiona.

### `backendpanol/.env.example`

- Uso: plantilla publica para saber que variables existen.
- Estado en git: versionado.
- No debe contener secretos reales.

### `Producto/.env`

- Uso: archivo de entorno para `docker compose` del nivel `Producto` (backend + frontend).
- Se crea copiando `Producto/.env.example`.
- Estado en git: normalmente local, no se debe subir con secretos reales.

### `Producto/.env.example`

- Uso: plantilla para el compose de `Producto`.
- Incluye placeholders (`replace_me`) y valores de ejemplo.

## Variables del backend (`backendpanol/.env.local`)

### Runtime

- `APP_PORT`: puerto HTTP del backend (`server.port`).

### Base de datos de runtime (Spring datasource)

- `DB_HOST`: host de PostgreSQL.
- `DB_PORT`: puerto de PostgreSQL.
- `DB_NAME`: nombre de base de datos.
- `DB_USER`: usuario de base de datos.
- `DB_SSL_MODE`: modo SSL de PostgreSQL (`disable`, `require`, etc).
- `DB_PASSWORD`: password de runtime del datasource.
  - Recomendado: definirlo en `secrets/application-secrets.properties`, no en `.env.local`.

### Seguridad JWT

- `JWT_ISSUER_URI`: issuer esperado por Spring Security para validar JWT.
- `SUPABASE_URL` (opcional): fallback para construir issuer si no hay `JWT_ISSUER_URI`.
- `VITE_SUPABASE_URL`: fallback adicional para issuer y configuracion compartida.
- `VITE_SUPABASE_PUBLISHABLE_KEY`: clave publica del proyecto Supabase (no es service role key).

### jOOQ Code Generation (build-time)

- `JOOQ_DB_URL`: JDBC URL para introspeccion del esquema al generar clases jOOQ.
- `JOOQ_DB_USER`: usuario para codegen jOOQ.
- `JOOQ_DB_PASSWORD`: password para codegen jOOQ.

Nota: estas 3 variables son usadas por Maven (`jooq-codegen-maven`) durante `generate-sources`/`compile`.

## Ejecucion de jOOQ

Esta seccion aplica cuando necesitas regenerar las clases de jOOQ desde la base PostgreSQL (por cambios de esquema o primera compilacion en una maquina nueva).

### Requisitos

1. JDK 21 activo (`java -version`).
2. Credenciales jOOQ validas:
   - `JOOQ_DB_URL`
   - `JOOQ_DB_USER`
   - `JOOQ_DB_PASSWORD`
3. Acceso de red a la base configurada en `JOOQ_DB_URL`.

### Como ejecutarlo

Desde `Producto/backendpanol`:

```bash
./mvnw generate-sources
```

Comando recomendado en PowerShell (carga `.env.local` y `secrets/application-secrets.properties` automaticamente):

```powershell
.\scripts\generate-jooq.ps1
```

En PowerShell (si quieres setear variables solo para esa sesion):

```powershell
$env:JOOQ_DB_URL="jdbc:postgresql://host:5432/postgres?sslmode=require"
$env:JOOQ_DB_USER="usuario"
$env:JOOQ_DB_PASSWORD="password"
./mvnw generate-sources
```

En Windows tambien puedes usar:

```powershell
.\mvnw.cmd generate-sources
```

### Cuando se ejecuta automaticamente

- El plugin de jOOQ esta en fase Maven `generate-sources`.
- Por eso tambien corre al hacer `./mvnw compile` o `./mvnw package`.

### Donde quedan los archivos generados

- Ruta generada: `backendpanol/target/generated-sources/jooq`
- Paquete base: `com.panol_project.backendpanol.jooq`

Si el IDE sigue mostrando imports en rojo despues de generar, recarga el proyecto Maven para que tome `target/generated-sources/jooq` como source root.

### Errores comunes y causa probable

- `Cannot execute query. No JDBC Connection configured`
  - Falta alguna `JOOQ_DB_*` o tiene valor invalido.
- `release version 21 not supported`
  - Maven esta usando un JDK menor a 21.
- Timeout o conexion rechazada a Postgres
  - URL/puerto/firewall incorrecto o credenciales sin permiso de introspeccion.

## Archivos de secrets en `backendpanol/secrets/`

### `application-secrets.properties`

- Cargado por Spring via `spring.config.import=optional:file:./secrets/application-secrets.properties`.
- Uso: secretos solo backend (por ejemplo `DB_PASSWORD`, opcionalmente `JWT_ISSUER_URI`).
- Estado en git: ignorado y no versionado.

Ejemplo:

```properties
DB_PASSWORD=replace_me
# JWT_ISSUER_URI=https://tu-proyecto.supabase.co/auth/v1
```

### `db_password.txt`

- Uso: password del contenedor `postgres` en `backendpanol/docker-compose.yaml`.
- Se monta como Docker secret (`POSTGRES_PASSWORD_FILE=/run/secrets/db_password`).
- Estado en git: ignorado y no versionado.

## Mapa de carga de configuracion

1. Spring Boot toma variables de entorno (`DB_HOST`, `DB_PORT`, etc).
2. Spring importa `./secrets/application-secrets.properties` si existe.
3. Para datasource:
   - URL: `jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}?sslmode=${DB_SSL_MODE}`
   - Usuario: `${DB_USER}`
   - Password: `${DB_PASSWORD}`
4. Para JWT issuer:
   - Primero `JWT_ISSUER_URI`.
   - Si no existe: `${SUPABASE_URL}/auth/v1`.
   - Si tampoco existe: `${VITE_SUPABASE_URL}/auth/v1`.

## Que se versiona y que no

Se versiona:

- `backendpanol/.env.example`
- `backendpanol/.env.local.example`
- `backendpanol/secrets/README.md`

No se versiona:

- `backendpanol/.env.local`
- `backendpanol/secrets/application-secrets.properties`
- `backendpanol/secrets/*.txt` (incluye `db_password.txt`)

## Recomendaciones

1. No reutilizar el mismo password en `db_password.txt`, `DB_PASSWORD` y `JOOQ_DB_PASSWORD` en ambientes reales.
2. Nunca subir secretos reales a Git.
3. Rotar inmediatamente credenciales que hayan sido compartidas por chat, commit o captura.
