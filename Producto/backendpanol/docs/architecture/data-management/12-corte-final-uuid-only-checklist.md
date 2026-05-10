# Corte Final UUID-Only: Checklist operativo

## Objetivo
Retirar compatibilidad legacy (`id` numérico público) y dejar contratos externos solo con UUID.

## Estado aplicado en este paso
- Frontend activo consume únicamente `/api/v2/**`.
- Endpoints legacy de catálogo/usuarios/auth quedaron fuera de superficie pública (`/internal/legacy/**`).
- Seguridad ya no permite excepciones legacy de `/api/v1/auth/login` ni `/api/categorias/**`.

## Precondiciones antes del corte destructivo DB
1. Backend y frontend desplegados en versión UUID-only.
2. Smoke test completo aprobado en `/api/v2/**`.
3. Snapshot + dump de PostgreSQL y export Mongo listos.
4. Ventana de mantenimiento aprobada.

## Script de referencia
- SQL manual: [uuid-final-cutover.sql](/c:/Users/cesar/OneDrive/Desktop/panol-project/panol_EscuelaSalud/MA_TPY1101_Seccion001D_Grupo5/Producto/backendpanol/docs/architecture/sql/uuid-final-cutover.sql)

## Validaciones post-corte
1. Login `/api/v2/auth/login`.
2. Director: CRUD usuarios.
3. Inventario: listar/crear/editar implementos.
4. Stock: entradas/movimientos/individuales.
5. Movimientos y etiquetas PDF.
6. Dashboard Director sin errores de carga.

